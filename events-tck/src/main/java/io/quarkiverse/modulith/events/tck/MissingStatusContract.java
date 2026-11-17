package io.quarkiverse.modulith.events.tck;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.FailedCriteria;
import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * The contract of {@link EventPublicationRepository} for rows without status, as Spring Modulith 1.x wrote them and as
 * a version 1 table keeps them after the migration to the version 2 structure. Such a row has the status
 * {@code COMPLETED} when it has a completion date and {@code FAILED} otherwise, which is how Spring Modulith 2.1.2
 * resubmits it. Stores that can hold such rows extend this class, in completion mode {@code update}, and write the
 * rows in {@link #insertWithoutStatus}. Parameterized cases take status names rather than {@link Status} values, so the
 * contract also runs in a {@code QuarkusExtensionTest}, whose application class loader loads its own {@link Status}.
 */
public abstract class MissingStatusContract {

    protected static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
    protected static final PublicationTargetIdentifier LISTENER = PublicationTargetIdentifier.of("billing.on-order");

    private static final Instant LATER = T0.plusSeconds(3600);

    /**
     * @return the repository under test, in completion mode {@code update}, without publications
     */
    protected abstract EventPublicationRepository repository();

    /**
     * Writes a publication of a {@link TckEvent} with the given id for {@link #LISTENER}, published at {@link #T0},
     * without status, completion attempts and last resubmission date, the way Spring Modulith 1.x left it.
     *
     * @param completionDate the completion date, empty for an incomplete publication
     * @return the identifier of the publication
     */
    protected abstract UUID insertWithoutStatus(String eventId, Optional<Instant> completionDate);

    private UUID insertIncomplete(String eventId) {
        return insertWithoutStatus(eventId, Optional.empty());
    }

    @Test
    void readsAnIncompleteRowWithoutStatusAsFailed() {
        // arrange
        UUID id = insertIncomplete("open");

        // act
        List<TargetEventPublication> incomplete = repository().findIncompletePublications();

        // assert
        assertThat(Publications.single(incomplete)).satisfies(p -> {
            assertThat(p.getIdentifier()).isEqualTo(id);
            assertThat(p.getEvent()).isEqualTo(new TckEvent("open"));
            assertThat(p.getTargetIdentifier()).isEqualTo(LISTENER);
            assertThat(p.getPublicationDate()).isEqualTo(T0);
            assertThat(p.getStatus()).isEqualTo(Status.FAILED);
            assertThat(p.getCompletionDate()).isEmpty();
            assertThat(p.getCompletionAttempts()).isZero();
            assertThat(p.getLastResubmissionDate()).isEmpty();
        });
    }

    @Test
    void readsACompletedRowWithoutStatusAsCompleted() {
        // arrange
        insertWithoutStatus("done", Optional.of(T0.plusSeconds(5)));

        // act
        List<TargetEventPublication> completed = repository().findCompletedPublications();

        // assert
        assertThat(Publications.single(completed)).satisfies(p -> {
            assertThat(p.getStatus()).isEqualTo(Status.COMPLETED);
            assertThat(p.getCompletionDate()).hasValue(T0.plusSeconds(5));
        });
    }

    @Test
    void findsIncompleteRowsWithoutStatusAmongTheFailedPublications() {
        // arrange
        insertIncomplete("open");
        insertWithoutStatus("done", Optional.of(T0.plusSeconds(5)));
        repository().create(TargetEventPublication.of(new TckEvent("published"), LISTENER, T0));

        // act
        List<TargetEventPublication> failed = repository().findFailedPublications(new FailedCriteria(10, LATER));

        // assert
        assertThat(Publications.events(failed)).containsExactly("open");
    }

    @Test
    void findsAndCountsRowsWithoutStatusByTheirStatus() {
        // arrange
        insertIncomplete("open");
        insertWithoutStatus("done", Optional.of(T0.plusSeconds(5)));

        // act
        List<TargetEventPublication> failed = repository().findByStatus(Status.FAILED);
        List<TargetEventPublication> completed = repository().findByStatus(Status.COMPLETED);

        // assert
        assertThat(Publications.events(failed)).containsExactly("open");
        assertThat(Publications.events(completed)).containsExactly("done");
        assertThat(repository().countByStatus(Status.FAILED)).isEqualTo(1);
        assertThat(repository().countByStatus(Status.COMPLETED)).isEqualTo(1);
        assertThat(repository().countByStatus(Status.PUBLISHED)).isZero();
        assertThat(repository().countByStatus(Status.PROCESSING)).isZero();
    }

    @ParameterizedTest
    @CsvSource({ "FAILED, true", "PUBLISHED, false", "PROCESSING, false" })
    void claimsAnIncompleteRowWithoutStatusAsFailed(String eligible, boolean expected) {
        // arrange
        UUID id = insertIncomplete("open");

        // act
        boolean claimed = repository().claim(id, Set.of(Status.valueOf(eligible)), LATER, T0.plusSeconds(10));

        // assert
        assertThat(claimed).isEqualTo(expected);
        assertThat(Publications.single(repository().findIncompletePublications()).getStatus())
                .isEqualTo(expected ? Status.RESUBMITTED : Status.FAILED);
    }

    @ParameterizedTest
    @CsvSource({ "FAILED, 1", "PUBLISHED, 0" })
    void readsTheRowsWithoutStatusOfAListenerAsFailed(String status, int expected) {
        // arrange
        insertIncomplete("open");

        // act
        List<TargetEventPublication> page = repository().findIncompletePublications(LISTENER, Set.of(Status.valueOf(status)),
                PageCriteria.first(LATER, 10));

        // assert
        assertThat(page).hasSize(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = { "PUBLISHED", "PROCESSING", "RESUBMITTED" })
    void leavesRowsWithoutStatusToTheFailedResubmissionNotToTheStalenessMonitor(String monitored) {
        // arrange
        insertIncomplete("open");

        // act
        int marked = repository().markStalePublicationsFailed(Status.valueOf(monitored), LATER);

        // assert
        assertThat(marked).isZero();
        assertThat(Publications.events(repository().findFailedPublications(FailedCriteria.all())))
                .containsExactly("open");
    }

    @Test
    void movesRowsWithoutStatusThroughTheLifecycle() {
        // arrange
        UUID id = insertIncomplete("open");

        // act
        boolean resubmitted = repository().markResubmitted(id, T0.plusSeconds(10));
        boolean processing = repository().markProcessing(id);
        TargetEventPublication delivering = Publications.single(repository().findIncompletePublications());
        repository().markCompleted(id, T0.plusSeconds(20));

        // assert
        assertThat(resubmitted).isTrue();
        assertThat(processing).isTrue();
        assertThat(delivering.getStatus()).isEqualTo(Status.PROCESSING);
        assertThat(delivering.getCompletionAttempts()).isEqualTo(1);
        assertThat(delivering.getLastResubmissionDate()).hasValue(T0.plusSeconds(10));
        assertThat(repository().findIncompletePublications()).isEmpty();
        assertThat(Publications.single(repository().findCompletedPublications()).getStatus())
                .isEqualTo(Status.COMPLETED);
    }

    @Test
    void findsTheIncompleteRowWithoutStatusOfAnEventForTheListener() {
        // arrange
        UUID id = insertIncomplete("open");

        // act
        Optional<TargetEventPublication> found = repository()
                .findIncompletePublicationsByEventAndTargetIdentifier(new TckEvent("open"), LISTENER);

        // assert
        assertThat(found).map(TargetEventPublication::getIdentifier).hasValue(id);
    }
}
