package io.quarkiverse.modulith.events.tck;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * The contract of a repository in {@code delete} completion mode: completed publications are gone.
 */
public abstract class DeleteCompletionModeContract extends CompletionModeContract {

    @Test
    void completedPublicationsAreDeleted() {
        // arrange
        TargetEventPublication publication = create("e-1", 0);

        // act
        repository().markCompleted(publication.getIdentifier(), T0.plusSeconds(5));

        // assert
        assertThat(repository().findCompletedPublications()).isEmpty();
        assertThat(repository().findByStatus(Status.COMPLETED)).isEmpty();
        assertThat(repository().countByStatus(Status.COMPLETED)).isZero();
    }
}
