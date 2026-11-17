package io.quarkiverse.modulith.events.tck;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * The contract of a repository in {@code delete} or {@code archive} completion mode: completing a publication takes it
 * out of the publication table. Stores extend {@link DeleteCompletionModeContract} or
 * {@link ArchiveCompletionModeContract}, in addition to {@link EventPublicationRepositoryContract} for the
 * {@code update} mode.
 */
public abstract class CompletionModeContract {

    protected static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
    protected static final PublicationTargetIdentifier LISTENER = PublicationTargetIdentifier.of("billing.on-order");

    /**
     * @return the repository under test, in the mode of the subclass, without publications
     */
    protected abstract EventPublicationRepository repository();

    protected TargetEventPublication create(String eventId, int secondsAfterT0) {
        return repository().create(TargetEventPublication.of(new TckEvent(eventId), LISTENER,
                T0.plusSeconds(secondsAfterT0)));
    }

    @Test
    void completingRemovesThePublicationFromTheIncompleteOnes() {
        // arrange
        TargetEventPublication completed = create("completed", 0);
        create("open", 1);
        repository().markProcessing(completed.getIdentifier());

        // act
        repository().markCompleted(completed.getIdentifier(), T0.plusSeconds(5));

        // assert
        assertThat(Publications.events(repository().findIncompletePublications()))
                .containsExactly("open");
        assertThat(repository().markProcessing(completed.getIdentifier())).isFalse();
        assertThat(repository().markResubmitted(completed.getIdentifier(), T0.plusSeconds(6))).isFalse();
        assertThat(repository().countByStatus(Status.PUBLISHED)).isEqualTo(1);
    }

    protected static List<String> events(List<TargetEventPublication> publications) {
        return Publications.events(publications);
    }
}
