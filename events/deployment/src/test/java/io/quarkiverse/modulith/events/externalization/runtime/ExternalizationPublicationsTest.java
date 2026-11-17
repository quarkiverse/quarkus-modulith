package io.quarkiverse.modulith.events.externalization.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.externalization.fakes.FixedInstance;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;

class ExternalizationPublicationsTest {

    private final InMemoryEventPublicationRepository repository = new InMemoryEventPublicationRepository();
    private final ExternalizationPublications publications = TestPublications
            .initialize(new ExternalizationPublications(), Optional.of(repository));

    @Test
    void storesNothingWithoutRepository() {
        // arrange
        ExternalizationPublications volatileOnly = TestPublications.initialize(new ExternalizationPublications(),
                Optional.empty());

        // act
        Optional<UUID> stored = volatileOnly.store("event", "listener");

        // assert
        assertThat(volatileOnly.isPersistent()).isFalse();
        assertThat(stored).isEmpty();
    }

    @Test
    void usesTheSystemClockWithoutClockBean() {
        // arrange
        ExternalizationPublications systemClock = new ExternalizationPublications();
        systemClock.repositories = new FixedInstance<>(Optional.of(repository));
        systemClock.clocks = new FixedInstance<>(Optional.empty());
        Instant before = Instant.now();

        // act
        systemClock.init();
        UUID stored = systemClock.store("event", "listener").orElseThrow();

        // assert
        assertThat(repository.findIncompletePublications()).singleElement()
                .satisfies(publication -> assertThat(publication.getIdentifier()).isEqualTo(stored))
                .satisfies(publication -> assertThat(publication.getPublicationDate()).isAfterOrEqualTo(before));
    }

    @Test
    void walksThePublicationThroughItsLifecycle() {
        // act
        UUID stored = publications.store("event", "listener").orElseThrow();
        publications.markProcessing(stored);
        TargetEventPublication processing = repository.findIncompletePublications().get(0);
        publications.markFailed(stored);
        TargetEventPublication failed = repository.findIncompletePublications().get(0);
        boolean claimed = publications.claim(stored, Duration.ofMinutes(1));
        boolean claimedTwice = publications.claim(stored, Duration.ofMinutes(1));
        publications.markCompleted(stored);

        // assert
        assertThat(publications.isPersistent()).isTrue();
        assertThat(processing.getStatus()).isEqualTo(Status.PROCESSING);
        assertThat(processing.getCompletionAttempts()).isEqualTo(1);
        assertThat(processing.getPublicationDate()).isEqualTo(TestPublications.NOW);
        assertThat(processing.getListenerId()).isEqualTo("listener");
        assertThat(failed.getStatus()).isEqualTo(Status.FAILED);
        assertThat(claimed).isTrue();
        assertThat(claimedTwice).isFalse();
        assertThat(repository.findCompletedPublications()).singleElement()
                .satisfies(publication -> assertThat(publication.getCompletionDate()).contains(TestPublications.NOW));
    }

    @Test
    void findsPendingPublicationsOfTheListenerOldestFirst() {
        // arrange
        TargetEventPublication newer = repository.create(publication("newer", "outbox", 20));
        TargetEventPublication older = repository.create(publication("older", "outbox", 10));
        TargetEventPublication failed = repository.create(publication("failed", "outbox", 30));
        repository.markFailed(failed.getIdentifier());
        TargetEventPublication processing = repository.create(publication("processing", "outbox", 5));
        repository.markProcessing(processing.getIdentifier());
        TargetEventPublication resubmitted = repository.create(publication("resubmitted", "outbox", 6));
        repository.markResubmitted(resubmitted.getIdentifier(), TestPublications.NOW);
        repository.create(publication("other", "listener", 1));

        // act
        List<TargetEventPublication> first = publications.findPending("outbox",
                PageCriteria.first(Instant.MAX, 3));
        List<TargetEventPublication> second = publications.findPending("outbox",
                PageCriteria.first(Instant.MAX, 3).next(first.get(2)));

        // assert
        assertThat(first).extracting(TargetEventPublication::getIdentifier).containsExactly(
                processing.getIdentifier(), resubmitted.getIdentifier(), older.getIdentifier());
        assertThat(second).extracting(TargetEventPublication::getIdentifier).containsExactly(
                newer.getIdentifier(), failed.getIdentifier());
    }

    private static TargetEventPublication publication(String event, String listener, int seconds) {
        return TargetEventPublication.of(event, PublicationTargetIdentifier.of(listener),
                TestPublications.NOW.plusSeconds(seconds));
    }
}
