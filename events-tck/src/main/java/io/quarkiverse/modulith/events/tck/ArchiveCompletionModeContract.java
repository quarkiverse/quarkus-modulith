package io.quarkiverse.modulith.events.tck;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * The contract of a repository in {@code archive} completion mode: completed publications move to the archive, and
 * every query about completed publications reads the archive.
 */
public abstract class ArchiveCompletionModeContract extends CompletionModeContract {

    @Test
    void completedPublicationsMoveToTheArchive() {
        // arrange
        TargetEventPublication publication = create("e-1", 0);
        repository().markProcessing(publication.getIdentifier());

        // act
        repository().markCompleted(publication.getIdentifier(), T0.plusSeconds(5));

        // assert
        assertThat(Publications.single(repository().findCompletedPublications())).satisfies(p -> {
            assertThat(p.getIdentifier()).isEqualTo(publication.getIdentifier());
            assertThat(p.getEvent()).isEqualTo(new TckEvent("e-1"));
            assertThat(p.getStatus()).isEqualTo(Status.COMPLETED);
            assertThat(p.getCompletionDate()).hasValue(T0.plusSeconds(5));
            assertThat(p.getCompletionAttempts()).isEqualTo(1);
        });
        assertThat(events(repository().findByStatus(Status.COMPLETED))).containsExactly("e-1");
        assertThat(repository().countByStatus(Status.COMPLETED)).isEqualTo(1);
        assertThat(events(repository().findCompletedPublications(PageCriteria.first(T0.plusSeconds(1), 10))))
                .containsExactly("e-1");
    }

    @Test
    void countsArchivedPublicationsItDeletesByIdentifier() {
        // arrange
        TargetEventPublication archived = create("archived", 0);
        TargetEventPublication open = create("open", 1);
        repository().markCompleted(archived.getIdentifier(), T0.plusSeconds(5));

        // act
        int deleted = repository().deletePublicationsCounting(List.of(archived.getIdentifier(), open.getIdentifier()));

        // assert
        assertThat(deleted).isEqualTo(2);
        assertThat(repository().findCompletedPublications()).isEmpty();
        assertThat(repository().findIncompletePublications()).isEmpty();
    }

    @Test
    void deletesFromTheArchive() {
        // arrange
        TargetEventPublication old = create("old", 0);
        TargetEventPublication recent = create("recent", 1);
        TargetEventPublication byId = create("by-id", 2);
        repository().markCompleted(old.getIdentifier(), T0.plusSeconds(10));
        repository().markCompleted(recent.getIdentifier(), T0.plusSeconds(30));
        repository().markCompleted(byId.getIdentifier(), T0.plusSeconds(30));

        // act
        int deleted = repository().deleteCompletedPublicationsCompletedBefore(T0.plusSeconds(20));
        repository().deletePublications(List.of(byId.getIdentifier()));
        repository().markCompleted(UUID.randomUUID(), T0.plusSeconds(40));
        List<String> remaining = events(repository().findCompletedPublications());
        repository().deleteCompletedPublications();

        // assert
        assertThat(deleted).isEqualTo(1);
        assertThat(remaining).as("deleting by identifier also deletes archived publications")
                .containsExactly("recent");
        assertThat(repository().findCompletedPublications()).isEmpty();
    }
}
