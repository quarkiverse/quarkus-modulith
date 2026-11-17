package io.quarkiverse.modulith.events.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.spi.EventRedelivery;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;
import io.quarkiverse.modulith.events.tck.Publications;
import io.quarkiverse.modulith.events.tck.TckEvent;

class EventPublicationJobsTest {

    static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");

    final InMemoryEventPublicationRepository repository = new InMemoryEventPublicationRepository();
    final RecordingRedelivery redelivery = new RecordingRedelivery(Set.of("l"));
    final RecordingPeriodicJobs scheduler = new RecordingPeriodicJobs();

    @ParameterizedTest
    @CsvSource({ "true, , 1", "false, true, 1", "false, false, 0", "false, , 0" })
    void resubmitsTheBacklogInTheBackgroundWhenAskedTo(boolean configured, Boolean legacy, int runs) {
        // arrange
        FakeEventsConfig config = FakeEventsConfig.defaults().withJobs(configured, Optional.empty(), Optional.empty());
        TestJobs jobs = jobs(config, Optional.ofNullable(legacy), Optional.of(scheduler));
        publication("backlog", NOW.minusSeconds(10));

        // act
        jobs.onStart(null);

        // assert
        assertThat(jobs.background).hasSize(runs);
        assertThat(redelivery.redelivered).hasSize(runs);
        assertThat(scheduler.intervals).isEmpty();
    }

    @Test
    void schedulesTheConfiguredJobsAndRunsThem() {
        // arrange
        FakeEventsConfig config = FakeEventsConfig.defaults()
                .withJobs(false, Optional.of(Duration.ofSeconds(30)), Optional.of(Duration.ofHours(1)))
                .withStaleness(new FakeStaleness(Optional.of(Duration.ofMinutes(5)), Optional.empty(), Optional.empty(),
                        Duration.ofSeconds(20)));
        TestJobs jobs = jobs(config, Optional.empty(), Optional.of(scheduler));
        publication("old", NOW.minusSeconds(3600));
        TargetEventPublication done = publication("done", NOW.minusSeconds(3600));
        repository.markCompleted(done.getIdentifier(), NOW.minusSeconds(30 * 24 * 3600));

        // act
        jobs.onStart(null);
        scheduler.tasks.get("quarkus-modulith-staleness").run();
        scheduler.tasks.get("quarkus-modulith-republish").run();
        scheduler.tasks.get("quarkus-modulith-delete-completed").run();

        // assert
        assertThat(scheduler.intervals).containsExactlyInAnyOrderEntriesOf(Map.of("quarkus-modulith-republish",
                Duration.ofSeconds(30), "quarkus-modulith-delete-completed", Duration.ofHours(1),
                "quarkus-modulith-staleness", Duration.ofSeconds(20)));
        assertThat(redelivery.redelivered).containsExactly("old");
        assertThat(repository.findCompletedPublications()).isEmpty();
        assertThat(Publications.events(repository.findByStatus(Status.RESUBMITTED))).containsExactly("old");
    }

    @ParameterizedTest
    @CsvSource({ "true, false, false", "false, true, false", "false, false, true" })
    void monitorsStalenessWhenAnyThresholdIsSet(boolean published, boolean processing, boolean resubmitted) {
        // arrange
        Duration threshold = Duration.ofMinutes(1);
        FakeEventsConfig config = FakeEventsConfig.defaults().withStaleness(new FakeStaleness(
                Optional.of(threshold).filter(t -> published), Optional.of(threshold).filter(t -> processing),
                Optional.of(threshold).filter(t -> resubmitted), Duration.ofSeconds(15)));
        TestJobs jobs = jobs(config, Optional.empty(), Optional.of(scheduler));

        // act
        jobs.onStart(null);

        // assert
        assertThat(scheduler.intervals).containsExactlyEntriesOf(Map.of("quarkus-modulith-staleness",
                Duration.ofSeconds(15)));
    }

    @Test
    void warnsInsteadOfSchedulingWithoutTheScheduler() {
        // arrange
        FakeEventsConfig config = FakeEventsConfig.defaults()
                .withJobs(false, Optional.of(Duration.ofSeconds(30)), Optional.empty());
        TestJobs jobs = jobs(config, Optional.empty(), Optional.empty());

        // act
        jobs.onStart(null);

        // assert
        assertThat(scheduler.intervals).isEmpty();
        assertThat(jobs.background).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "PT0S, published", "PT0S, processing", "PT0S, resubmitted", "-PT1S, published", "-PT1S, processing",
            "-PT1S, resubmitted" })
    void rejectsStalenessThresholdsThatAreNotPositive(String threshold, String status) {
        // arrange
        Optional<Duration> invalid = Optional.of(Duration.parse(threshold));
        FakeStaleness staleness = new FakeStaleness(invalid.filter(d -> status.equals("published")),
                invalid.filter(d -> status.equals("processing")), invalid.filter(d -> status.equals("resubmitted")),
                Duration.ofMinutes(1));
        TestJobs jobs = jobs(FakeEventsConfig.defaults().withStaleness(staleness), Optional.empty(),
                Optional.of(scheduler));

        // act & assert
        assertThatThrownBy(() -> jobs.onStart(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quarkus.modulith.events.staleness thresholds must be positive, got " + invalid.get());
    }

    @ParameterizedTest
    @CsvSource({ "PT0S", "-PT1M" })
    void rejectsACheckIntervalThatIsNotPositive(Duration interval) {
        // arrange
        FakeStaleness staleness = new FakeStaleness(Optional.empty(), Optional.empty(), Optional.empty(), interval);
        TestJobs jobs = jobs(FakeEventsConfig.defaults().withStaleness(staleness), Optional.empty(),
                Optional.of(scheduler));

        // act & assert
        assertThatThrownBy(() -> jobs.onStart(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quarkus.modulith.events.staleness.check-interval must be positive, got " + interval);
    }

    @ParameterizedTest
    @CsvSource({ "0", "-1" })
    void rejectsABatchSizeBelowOne(int batchSize) {
        // arrange
        TestJobs jobs = jobs(FakeEventsConfig.defaults().withBatchSize(batchSize), Optional.empty(),
                Optional.of(scheduler));

        // act & assert
        assertThatThrownBy(() -> jobs.onStart(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quarkus.modulith.events.republish-batch-size must be at least 1, got " + batchSize);
    }

    @Test
    void keepsTheScheduleAliveAndRemembersFailingJobsUntilTheyRecover() {
        // arrange
        TestJobs jobs = jobs(FakeEventsConfig.defaults(), Optional.empty(), Optional.of(scheduler));
        Runnable failing = () -> {
            throw new IllegalStateException("store down");
        };
        List<String> states = new ArrayList<>();

        // act
        jobs.run("failing job", failing);
        states.add(String.join(",", jobs.failing));
        jobs.run("failing job", failing);
        states.add(String.join(",", jobs.failing));
        jobs.run("failing job", () -> {
        });
        states.add(String.join(",", jobs.failing));
        jobs.run("failing job", () -> {
        });
        states.add(String.join(",", jobs.failing));

        // assert
        assertThat(states).containsExactly("failing job", "failing job", "", "");
        assertThat(EventPublicationJobs.LEGACY_REPUBLISH_KEY)
                .isEqualTo("quarkus.modulith.republish-outstanding-events-on-restart");
    }

    TestJobs jobs(FakeEventsConfig config, Optional<Boolean> legacy, Optional<PeriodicJobs> periodicJobs) {
        TestRegistry registry = new TestRegistry(repository, config, Clock.fixed(NOW, ZoneOffset.UTC),
                Optional.<EventRedelivery> of(redelivery));
        return new TestJobs(registry, config, legacy, periodicJobs);
    }

    TargetEventPublication publication(String id, Instant publicationDate) {
        return repository.create(TargetEventPublication.of(new TckEvent(id), PublicationTargetIdentifier.of("l"),
                publicationDate));
    }
}
