package io.quarkiverse.modulith.events.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.events.EventPublication;
import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.ResubmissionOptions;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;
import io.quarkiverse.modulith.events.tck.Publications;
import io.quarkiverse.modulith.events.tck.TckEvent;

class DefaultEventPublicationRegistryTest {

    static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");
    static final String LISTENER = "billing.on-order";

    final InMemoryEventPublicationRepository repository = new InMemoryEventPublicationRepository();
    final RecordingRedelivery redelivery = new RecordingRedelivery(Set.of(LISTENER));
    final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void storesOnePublicationPerListenerAndDrivesItsLifecycle() {
        // arrange
        DefaultEventPublicationRegistry registry = registry(FakeEventsConfig.defaults());

        // act
        List<UUID> ids = registry.store(new TckEvent("e-1"), List.of(LISTENER, "mail.on-order"));
        boolean claimed = registry.markProcessing(ids.get(0));
        registry.markCompleted(ids.get(0));
        registry.markFailed(ids.get(1), new IllegalStateException("boom"));

        // assert
        assertThat(claimed).isTrue();
        assertThat(Publications.single(repository.findCompletedPublications())).satisfies(p -> {
            assertThat(p.getIdentifier()).isEqualTo(ids.get(0));
            assertThat(p.getPublicationDate()).isEqualTo(NOW);
            assertThat(p.getCompletionDate()).hasValue(NOW);
            assertThat(p.getCompletionAttempts()).isEqualTo(1);
        });
        assertThat(Publications.single(repository.findIncompletePublications())).satisfies(p -> {
            assertThat(p.getIdentifier()).isEqualTo(ids.get(1));
            assertThat(p.getListenerId()).isEqualTo("mail.on-order");
            assertThat(p.getStatus()).isEqualTo(Status.FAILED);
        });
        assertThat(registry.findIncompletePublications()).extracting(EventPublication::getIdentifier)
                .containsExactly(ids.get(1));
        assertThat(registry.findAll()).extracting(EventPublication::getIdentifier).containsExactly(ids.get(0));
    }

    @Test
    void resubmitsIncompletePublicationsPageByPage() {
        // arrange
        DefaultEventPublicationRegistry registry = registry(FakeEventsConfig.defaults().withAbandonAfter(3));
        create("ok-1", LISTENER, 0, 0);
        create("ok-2", LISTENER, 1, 0);
        create("filtered", LISTENER, 2, 0);
        create("gone", "removed.listener", 3, 0);
        create("boom", LISTENER, 4, 0);
        create("abandoned", LISTENER, 5, 3);
        TargetEventPublication claimed = create("claimed", LISTENER, 6, 0);
        repository.markResubmitted(claimed.getIdentifier(), NOW);
        create("ok-3", LISTENER, 7, 0);

        // act
        int submitted = registry.resubmitIncompletePublications(p -> !((TckEvent) p.getEvent()).id().equals("filtered"));

        // assert
        assertThat(submitted).isEqualTo(3);
        assertThat(redelivery.redelivered).containsExactly("ok-1", "ok-2", "ok-3");
        assertThat(repository.findByStatus(Status.FAILED)).extracting(p -> ((TckEvent) p.getEvent()).id(),
                TargetEventPublication::getCompletionAttempts).containsExactly(tuple("gone", 0), tuple("boom", 1));
        assertThat(Publications.events(repository.findByStatus(Status.RESUBMITTED)))
                .containsExactly("ok-1", "ok-2", "claimed", "ok-3");
    }

    @Test
    void skipsPublicationsAnotherInstanceClaimedConcurrently() {
        // arrange
        ConflictingRepository conflicting = new ConflictingRepository("40001");
        WorkerRegistry registry = new WorkerRegistry(conflicting, new DirectTransactions(), FakeEventsConfig.defaults(),
                clock, redelivery);
        conflicting.create(TargetEventPublication.of(new TckEvent("a"), PublicationTargetIdentifier.of(LISTENER), NOW));
        conflicting.create(TargetEventPublication.of(new TckEvent("b"), PublicationTargetIdentifier.of(LISTENER), NOW));

        // act
        int submitted = registry.resubmitIncompletePublications(p -> true);

        // assert
        assertThat(submitted).isZero();
        assertThat(redelivery.redelivered).isEmpty();
    }

    @Test
    void propagatesOtherClaimFailures() {
        // arrange
        ConflictingRepository conflicting = new ConflictingRepository("08006");
        WorkerRegistry registry = new WorkerRegistry(conflicting, new DirectTransactions(), FakeEventsConfig.defaults(),
                clock, redelivery);
        conflicting.create(TargetEventPublication.of(new TckEvent("a"), PublicationTargetIdentifier.of(LISTENER), NOW));

        // act & assert
        assertThatThrownBy(() -> registry.resubmitIncompletePublications(p -> true))
                .isInstanceOf(IllegalStateException.class).hasMessage("claim failed");
    }

    @Test
    void leavesListenersOwnedByAnotherComponentAlone() {
        // arrange
        DefaultEventPublicationRegistry registry = registry(FakeEventsConfig.defaults());
        registry.exclusions = List.of(listener -> listener.value().equals("outbox"));
        create("drained", "outbox", 0, 0);
        create("normal", LISTENER, 1, 0);
        failedAt("drained-failed", NOW.minusSeconds(100), "outbox");

        // act
        int incomplete = registry.resubmitIncompletePublications(p -> true);
        int failed = registry.resubmit(ResubmissionOptions.defaults());

        // assert
        assertThat(incomplete).isEqualTo(1);
        assertThat(failed).isZero();
        assertThat(redelivery.redelivered).containsExactly("normal");
    }

    @ParameterizedTest
    @CsvSource({ "PT10M, ", ", PT10M" })
    void takesOverDeliveriesOlderThanTheStalenessThreshold(Duration processing, Duration resubmitted) {
        // arrange
        DefaultEventPublicationRegistry registry = registry(FakeEventsConfig.defaults().withStaleness(new FakeStaleness(
                Optional.empty(), Optional.ofNullable(processing), Optional.ofNullable(resubmitted),
                Duration.ofMinutes(1))));
        TargetEventPublication stuck = createAt("stuck", NOW.minusSeconds(3600));
        TargetEventPublication running = createAt("running", NOW.minusSeconds(60));
        repository.markProcessing(stuck.getIdentifier());
        repository.markProcessing(running.getIdentifier());

        // act
        int submitted = registry.resubmitIncompletePublications(p -> true);

        // assert
        assertThat(submitted).isEqualTo(1);
        assertThat(redelivery.redelivered).containsExactly("stuck");
    }

    @Test
    void takesOverDeliveriesOlderThanTheRepublishAgeWithoutAThreshold() {
        // arrange
        DefaultEventPublicationRegistry registry = registry(FakeEventsConfig.defaults());
        TargetEventPublication stuck = createAt("stuck", NOW.minusSeconds(3600));
        TargetEventPublication claimed = createAt("claimed", NOW.minusSeconds(3599));
        TargetEventPublication running = createAt("running", NOW.minusSeconds(60));
        repository.markProcessing(stuck.getIdentifier());
        repository.markResubmitted(claimed.getIdentifier(), NOW.minusSeconds(301));
        repository.markProcessing(running.getIdentifier());

        // act
        int submitted = registry.resubmitIncompletePublications(p -> true);

        // assert
        assertThat(registry.staleClaimThreshold()).isEqualTo(Duration.ofMinutes(5));
        assertThat(submitted).isEqualTo(2);
        assertThat(redelivery.redelivered).containsExactly("stuck", "claimed");
        assertThat(Publications.events(repository.findByStatus(Status.PROCESSING))).containsExactly("running");
    }

    @Test
    void leavesAPublicationClaimedWhenItCannotEvenBeMarkedFailed() {
        // arrange
        UnmarkableRepository unmarkable = new UnmarkableRepository();
        WorkerRegistry registry = new WorkerRegistry(unmarkable, new DirectTransactions(), FakeEventsConfig.defaults(),
                clock, redelivery);
        unmarkable.create(TargetEventPublication.of(new TckEvent("boom"), PublicationTargetIdentifier.of(LISTENER),
                NOW));

        // act
        int submitted = registry.resubmitIncompletePublications(p -> true);

        // assert
        assertThat(submitted).isZero();
        assertThat(Publications.events(unmarkable.findByStatus(Status.RESUBMITTED))).containsExactly("boom");
    }

    @Test
    void marksPublicationsFailedWhenNoDispatcherExists() {
        // arrange
        TestRegistry registry = new TestRegistry(repository, FakeEventsConfig.defaults(), clock, Optional.empty());
        create("e-1", LISTENER, 0, 0);

        // act
        int submitted = registry.resubmitIncompletePublications(p -> true);

        // assert
        assertThat(submitted).isZero();
        assertThat(Publications.events(repository.findByStatus(Status.FAILED))).containsExactly("e-1");
    }

    @Test
    void resubmitsOnlyPublicationsOlderThanTheGivenAge() {
        // arrange
        DefaultEventPublicationRegistry registry = registry(FakeEventsConfig.defaults());
        createAt("old", NOW.minusSeconds(600));
        createAt("recent", NOW.minusSeconds(60));

        // act
        int submitted = registry.resubmitIncompletePublicationsOlderThan(Duration.ofMinutes(5));

        // assert
        assertThat(submitted).isEqualTo(1);
        assertThat(redelivery.redelivered).containsExactly("old");
    }

    @ParameterizedTest
    @CsvSource({
            "10, 10, 0, 0, 0, 'f-1,f-2,f-3'",
            "10, 2, 0, 0, 0, 'f-1,f-2'",
            "2, 10, 1, 0, 0, 'f-1'",
            "2, 10, 0, 1, 0, 'f-1'",
            "2, 10, 1, 1, 0, ''",
            "1, 10, 1, 0, 0, ''",
            "10, 10, 0, 0, 450, 'f-1'"
    })
    void resubmitsFailedPublicationsWithinTheOptions(int maxInFlight, int batchSize, int alreadyResubmitted,
            int alreadyProcessing, int minAgeSeconds, String expected) {
        // arrange
        DefaultEventPublicationRegistry registry = registry(FakeEventsConfig.defaults());
        failedAt("f-1", NOW.minusSeconds(500));
        failedAt("f-2", NOW.minusSeconds(400));
        failedAt("f-3", NOW.minusSeconds(300));
        for (int i = 0; i < alreadyResubmitted; i++) {
            TargetEventPublication other = createAt("other-" + i, NOW.minusSeconds(900));
            repository.markResubmitted(other.getIdentifier(), NOW);
        }
        for (int i = 0; i < alreadyProcessing; i++) {
            TargetEventPublication other = createAt("running-" + i, NOW.minusSeconds(900));
            repository.markProcessing(other.getIdentifier());
        }
        ResubmissionOptions options = ResubmissionOptions.defaults().withMaxInFlight(maxInFlight)
                .withBatchSize(batchSize).withMinAge(Duration.ofSeconds(minAgeSeconds));

        // act
        int submitted = registry.resubmit(options);

        // assert
        List<String> expectedIds = expected.isEmpty() ? List.of() : List.of(expected.split(","));
        assertThat(submitted).isEqualTo(expectedIds.size());
        assertThat(redelivery.redelivered).containsExactlyElementsOf(expectedIds);
    }

    @Test
    void optionBasedIncompleteResubmissionResubmitsFailedPublicationsMatchingTheFilter() {
        // arrange
        DefaultEventPublicationRegistry registry = registry(FakeEventsConfig.defaults().withAbandonAfter(2));
        failedAt("keep", NOW.minusSeconds(10));
        failedAt("skip", NOW.minusSeconds(9));
        TargetEventPublication tired = failedAt("tired", NOW.minusSeconds(8));
        repository.markProcessing(tired.getIdentifier());
        repository.markProcessing(tired.getIdentifier());
        repository.markFailed(tired.getIdentifier());
        TargetEventPublication orphan = create("orphan", "removed.listener", 0, 0);
        repository.markFailed(orphan.getIdentifier());

        // act
        int submitted = registry.resubmitIncompletePublications(ResubmissionOptions.defaults()
                .withFilter(p -> !((TckEvent) p.getEvent()).id().equals("skip")));

        // assert
        assertThat(submitted).isEqualTo(1);
        assertThat(redelivery.redelivered).containsExactly("keep");
    }

    @Test
    void deletesCompletedPublicationsByPredicateAcrossPages() {
        // arrange
        DefaultEventPublicationRegistry registry = registry(FakeEventsConfig.defaults());
        for (int i = 0; i < 5; i++) {
            TargetEventPublication publication = create("c-" + i, LISTENER, i, 0);
            repository.markCompleted(publication.getIdentifier(), NOW);
        }
        create("open", LISTENER, 9, 0);

        // act
        int deleted = registry.deletePublications(p -> !((TckEvent) p.getEvent()).id().equals("c-3"));
        int none = registry.deletePublications(p -> false);

        // assert
        assertThat(deleted).isEqualTo(4);
        assertThat(none).isZero();
        assertThat(Publications.events(repository.findCompletedPublications())).containsExactly("c-3");
    }

    @Test
    void deletesCompletedPublicationsOlderThanTheGivenAge() {
        // arrange
        DefaultEventPublicationRegistry registry = registry(FakeEventsConfig.defaults());
        TargetEventPublication old = create("old", LISTENER, 0, 0);
        TargetEventPublication recent = create("recent", LISTENER, 1, 0);
        repository.markCompleted(old.getIdentifier(), NOW.minusSeconds(3600));
        repository.markCompleted(recent.getIdentifier(), NOW.minusSeconds(60));

        // act
        int deleted = registry.deletePublicationsOlderThan(Duration.ofMinutes(30));

        // assert
        assertThat(deleted).isEqualTo(1);
        assertThat(Publications.events(repository.findCompletedPublications())).containsExactly("recent");
    }

    @Test
    void listsAndDeletesAbandonedPublications() {
        // arrange
        DefaultEventPublicationRegistry registry = registry(FakeEventsConfig.defaults().withAbandonAfter(2));
        DefaultAbandonedEventPublications abandoned = new DefaultAbandonedEventPublications();
        abandoned.registry = registry;
        for (int i = 0; i < 3; i++) {
            create("tired-" + i, LISTENER, i, 2);
        }
        create("fresh", LISTENER, 5, 1);

        // act
        List<EventPublication> found = abandoned.findAll();
        int deleted = abandoned.deletePublications(p -> !((TckEvent) p.getEvent()).id().equals("tired-1"));

        // assert
        assertThat(found).extracting(p -> ((TckEvent) p.getEvent()).id()).containsExactly("tired-0", "tired-1",
                "tired-2");
        assertThat(deleted).isEqualTo(2);
        assertThat(Publications.events(repository.findIncompletePublications())).containsExactly("tired-1", "fresh");
    }

    @Test
    void listsAbandonedOutboxPublicationsThatResubmissionLeavesAlone() {
        // arrange
        DefaultEventPublicationRegistry registry = registry(FakeEventsConfig.defaults().withAbandonAfter(3));
        registry.exclusions = List.of(listener -> listener.value().equals("outbox"));
        create("rejected-by-the-broker", "outbox", 0, 3);
        create("still-retried", "outbox", 1, 2);

        // act
        List<EventPublication> found = registry.findAbandoned();

        // assert
        assertThat(found).extracting(p -> ((TckEvent) p.getEvent()).id()).containsExactly("rejected-by-the-broker");
    }

    @Test
    void findsNoAbandonedPublicationsWithoutAnAttemptLimit() {
        // arrange
        DefaultEventPublicationRegistry registry = registry(FakeEventsConfig.defaults());
        create("tired", LISTENER, 0, 99);

        // act
        List<EventPublication> found = registry.findAbandoned();

        // assert
        assertThat(found).isEmpty();
    }

    @Test
    void marksStalePublicationsOfMonitoredStatusesFailed() {
        // arrange
        DefaultEventPublicationRegistry registry = registry(FakeEventsConfig.defaults().withStaleness(new FakeStaleness(
                Optional.of(Duration.ofMinutes(10)), Optional.of(Duration.ofDays(1)), Optional.of(Duration.ofMinutes(1)),
                Duration.ofMinutes(1))));
        createAt("stale-published", NOW.minusSeconds(3600));
        createAt("fresh-published", NOW.minusSeconds(60));
        TargetEventPublication processing = createAt("old-processing", NOW.minusSeconds(3600));
        repository.markProcessing(processing.getIdentifier());
        TargetEventPublication resubmitted = createAt("stale-resubmitted", NOW.minusSeconds(3599));
        repository.markResubmitted(resubmitted.getIdentifier(), NOW.minusSeconds(120));

        // act
        int marked = registry.markStalePublicationsFailed();

        // assert
        assertThat(marked).isEqualTo(2);
        assertThat(Publications.events(repository.findByStatus(Status.FAILED)))
                .containsExactly("stale-published", "stale-resubmitted");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 100 })
    void reportsUnfinishedPublicationsAtShutdown(int unfinished) {
        // arrange
        DefaultEventPublicationRegistry registry = registry(FakeEventsConfig.defaults());
        for (int i = 0; i < unfinished; i++) {
            create("left-" + i, LISTENER, i, 0);
        }

        // act
        registry.onShutdown(null);

        // assert
        assertThat(repository.findIncompletePublications()).hasSize(unfinished);
    }

    @Test
    void survivesAStoreThatIsGoneAtShutdown() {
        // arrange
        FailingRepository failing = new FailingRepository();
        TestRegistry registry = new TestRegistry(failing, FakeEventsConfig.defaults(), clock, Optional.empty());
        failing.create(TargetEventPublication.of(new TckEvent("left"), PublicationTargetIdentifier.of(LISTENER), NOW));

        // act
        registry.onShutdown(null);

        // assert
        assertThat(failing.findIncompletePublications()).hasSize(1);
    }

    DefaultEventPublicationRegistry registry(FakeEventsConfig config) {
        return new WorkerRegistry(repository, new DirectTransactions(), config, clock, redelivery);
    }

    TargetEventPublication create(String id, String listener, int secondsBeforeNow, int attempts) {
        return repository.create(TargetEventPublication.of(UUID.randomUUID(), new TckEvent(id),
                TckEvent.class.getName(), PublicationTargetIdentifier.of(listener),
                NOW.minusSeconds(1000).plusSeconds(secondsBeforeNow), Optional.empty(), Status.PUBLISHED, attempts,
                Optional.empty()));
    }

    TargetEventPublication createAt(String id, Instant publicationDate) {
        return repository.create(TargetEventPublication.of(new TckEvent(id), PublicationTargetIdentifier.of(LISTENER),
                publicationDate));
    }

    TargetEventPublication failedAt(String id, Instant publicationDate) {
        return failedAt(id, publicationDate, LISTENER);
    }

    TargetEventPublication failedAt(String id, Instant publicationDate, String listener) {
        TargetEventPublication publication = repository.create(TargetEventPublication.of(new TckEvent(id),
                PublicationTargetIdentifier.of(listener), publicationDate));
        repository.markFailed(publication.getIdentifier());
        return publication;
    }
}
