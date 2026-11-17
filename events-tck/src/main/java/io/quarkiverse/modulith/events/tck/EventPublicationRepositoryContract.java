package io.quarkiverse.modulith.events.tck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.events.EventPublication;
import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.FailedCriteria;
import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * The contract of {@link EventPublicationRepository}. Every store extends this class in its tests and returns its
 * repository from {@link #repository()}, empty at the start of each test, the way Spring Modulith's stores share
 * {@code spring-modulith-events-tests}.
 * <p>
 * Dates are whole seconds, so stores that keep microseconds or milliseconds compare equal. Each test runs the calls
 * the way the registry does: one call at a time, each in its own transaction when the store is transactional.
 */
public abstract class EventPublicationRepositoryContract {

    protected static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
    protected static final PublicationTargetIdentifier LISTENER = PublicationTargetIdentifier.of("billing.on-order");
    protected static final Set<Status> ELIGIBLE = Set.of(Status.PUBLISHED, Status.FAILED);
    protected static final PublicationTargetIdentifier OTHER_LISTENER = PublicationTargetIdentifier.of("mail.on-order");

    /**
     * @return the repository under test, without publications
     */
    protected abstract EventPublicationRepository repository();

    /**
     * Whether the store keeps status, completion attempts and resubmission dates. A store on a table without those
     * columns (the legacy JDBC structure) returns {@code false}, which skips the tests about them.
     */
    protected boolean tracksStatus() {
        return true;
    }

    private void assumeStatus() {
        assumeTrue(tracksStatus(), "the store does not keep status, attempts or resubmission dates");
    }

    protected TargetEventPublication create(String eventId, PublicationTargetIdentifier target, int secondsAfterT0) {
        return repository().create(TargetEventPublication.of(new TckEvent(eventId), target,
                T0.plusSeconds(secondsAfterT0)));
    }

    @Test
    void createsPublishedPublications() {
        // arrange
        TckEvent event = new TckEvent("e-1");

        // act
        TargetEventPublication created = repository().create(TargetEventPublication.of(event, LISTENER, T0));

        // assert
        assertThat(repository().findIncompletePublications()).singleElement().satisfies(p -> {
            assertThat(p.getIdentifier()).isEqualTo(created.getIdentifier());
            assertThat(p.getEvent()).isEqualTo(event);
            assertThat(p.getEventType()).isEqualTo(TckEvent.class.getName());
            assertThat(p.getTargetIdentifier()).isEqualTo(LISTENER);
            assertThat(p.getListenerId()).isEqualTo(LISTENER.value());
            assertThat(p.getPublicationDate()).isEqualTo(T0);
            assertThat(p.getStatus()).isEqualTo(Status.PUBLISHED);
            assertThat(p.getCompletionDate()).isEmpty();
            if (tracksStatus()) {
                assertThat(p.getCompletionAttempts()).isZero();
                assertThat(p.getLastResubmissionDate()).isEmpty();
            }
        });
    }

    @Test
    void markingProcessingCountsAnAttempt() {
        // arrange
        assumeStatus();
        TargetEventPublication publication = create("e-1", LISTENER, 0);

        // act
        boolean first = repository().markProcessing(publication.getIdentifier());
        boolean second = repository().markProcessing(publication.getIdentifier());
        TargetEventPublication processing = single(repository().findIncompletePublications());
        repository().markCompleted(publication.getIdentifier(), T0.plusSeconds(1));
        boolean afterCompletion = repository().markProcessing(publication.getIdentifier());
        boolean unknown = repository().markProcessing(UUID.randomUUID());

        // assert
        assertThat(first).isTrue();
        assertThat(second).isTrue();
        assertThat(processing.getStatus()).isEqualTo(Status.PROCESSING);
        assertThat(processing.getCompletionAttempts()).isEqualTo(2);
        assertThat(afterCompletion).isFalse();
        assertThat(unknown).isFalse();
    }

    @Test
    void completedPublicationsMoveToTheCompletedListAndStayCompleted() {
        // arrange
        TargetEventPublication publication = create("e-1", LISTENER, 0);
        repository().markProcessing(publication.getIdentifier());

        // act
        repository().markCompleted(publication.getIdentifier(), T0.plusSeconds(5));
        repository().markCompleted(UUID.randomUUID(), T0.plusSeconds(5));
        repository().markFailed(publication.getIdentifier());

        // assert
        assertThat(repository().findIncompletePublications()).isEmpty();
        assertThat(single(repository().findCompletedPublications())).satisfies(p -> {
            assertThat(p.getStatus()).isEqualTo(Status.COMPLETED);
            assertThat(p.getCompletionDate()).hasValue(T0.plusSeconds(5));
            assertThat(p.isCompleted()).isTrue();
            if (tracksStatus()) {
                assertThat(p.getCompletionAttempts()).isEqualTo(1);
            }
        });
    }

    @Test
    void failedPublicationsKeepTheirAttempts() {
        // arrange
        assumeStatus();
        TargetEventPublication publication = create("e-1", LISTENER, 0);
        repository().markProcessing(publication.getIdentifier());

        // act
        repository().markFailed(publication.getIdentifier());
        repository().markFailed(UUID.randomUUID());

        // assert
        assertThat(single(repository().findIncompletePublications())).satisfies(p -> {
            assertThat(p.getStatus()).isEqualTo(Status.FAILED);
            assertThat(p.getCompletionAttempts()).isEqualTo(1);
        });
    }

    /**
     * Parity with Spring Modulith 2.1.2: {@code markResubmitted} only refuses a publication that is resubmitted
     * already, so it also changes one that is being delivered. Exclusive claims go through {@code claim}.
     */
    @Test
    void resubmissionClaimsAPublicationOnceWithoutCountingAnAttempt() {
        // arrange
        assumeStatus();
        TargetEventPublication failed = create("e-1", LISTENER, 0);
        TargetEventPublication completed = create("e-2", LISTENER, 1);
        repository().markFailed(failed.getIdentifier());
        repository().markCompleted(completed.getIdentifier(), T0.plusSeconds(2));

        // act
        boolean claimed = repository().markResubmitted(failed.getIdentifier(), T0.plusSeconds(10));
        boolean claimedTwice = repository().markResubmitted(failed.getIdentifier(), T0.plusSeconds(11));
        boolean completedClaimed = repository().markResubmitted(completed.getIdentifier(), T0.plusSeconds(10));
        boolean unknownClaimed = repository().markResubmitted(UUID.randomUUID(), T0.plusSeconds(10));
        TargetEventPublication resubmitted = single(repository().findIncompletePublications());
        boolean processing = repository().markProcessing(failed.getIdentifier());
        boolean claimedAgain = repository().markResubmitted(failed.getIdentifier(), T0.plusSeconds(12));

        // assert
        assertThat(claimed).isTrue();
        assertThat(claimedTwice).isFalse();
        assertThat(completedClaimed).isFalse();
        assertThat(unknownClaimed).isFalse();
        assertThat(resubmitted.getStatus()).isEqualTo(Status.RESUBMITTED);
        assertThat(resubmitted.getCompletionAttempts()).isZero();
        assertThat(resubmitted.getLastResubmissionDate()).hasValue(T0.plusSeconds(10));
        assertThat(resubmitted.getLastPublicationDate()).isEqualTo(T0.plusSeconds(10));
        assertThat(processing).isTrue();
        assertThat(claimedAgain).isTrue();
    }

    @Test
    void claimsEligiblePublicationsOnce() {
        // arrange
        assumeStatus();
        TargetEventPublication published = create("published", LISTENER, 0);
        TargetEventPublication failed = create("failed", LISTENER, 1);
        TargetEventPublication completed = create("completed", LISTENER, 2);
        repository().markFailed(failed.getIdentifier());
        repository().markCompleted(completed.getIdentifier(), T0.plusSeconds(3));

        // act
        boolean first = repository().claim(published.getIdentifier(), ELIGIBLE, T0, T0.plusSeconds(10));
        boolean again = repository().claim(published.getIdentifier(), ELIGIBLE, T0, T0.plusSeconds(11));
        boolean failedClaimed = repository().claim(failed.getIdentifier(), ELIGIBLE, T0, T0.plusSeconds(10));
        boolean completedClaimed = repository().claim(completed.getIdentifier(), ELIGIBLE, T0, T0.plusSeconds(10));
        boolean unknownClaimed = repository().claim(UUID.randomUUID(), ELIGIBLE, T0, T0.plusSeconds(10));

        // assert
        assertThat(first).isTrue();
        assertThat(again).isFalse();
        assertThat(failedClaimed).isTrue();
        assertThat(completedClaimed).isFalse();
        assertThat(unknownClaimed).isFalse();
        assertThat(repository().findIncompletePublications()).allSatisfy(p -> {
            assertThat(p.getStatus()).isEqualTo(Status.RESUBMITTED);
            assertThat(p.getLastResubmissionDate()).hasValue(T0.plusSeconds(10));
            assertThat(p.getCompletionAttempts()).isZero();
        }).hasSize(2);
    }

    @Test
    void neverClaimsAPublicationThatIsBeingDelivered() {
        // arrange
        assumeStatus();
        TargetEventPublication publication = create("e-1", LISTENER, 0);
        repository().claim(publication.getIdentifier(), ELIGIBLE, T0, T0.plusSeconds(10));
        repository().markProcessing(publication.getIdentifier());

        // act
        boolean second = repository().claim(publication.getIdentifier(), ELIGIBLE, T0.plusSeconds(10),
                T0.plusSeconds(11));

        // assert
        assertThat(second).isFalse();
        assertThat(single(repository().findIncompletePublications()).getStatus()).isEqualTo(Status.PROCESSING);
    }

    @Test
    void takesOverStaleClaimsAndDeliveries() {
        // arrange
        assumeStatus();
        TargetEventPublication claimed = create("claimed", LISTENER, 0);
        TargetEventPublication processing = create("processing", LISTENER, 1);
        repository().claim(claimed.getIdentifier(), ELIGIBLE, T0, T0.plusSeconds(10));
        repository().claim(processing.getIdentifier(), ELIGIBLE, T0, T0.plusSeconds(10));
        repository().markProcessing(processing.getIdentifier());

        // act
        boolean freshClaimed = repository().claim(claimed.getIdentifier(), ELIGIBLE, T0.plusSeconds(10),
                T0.plusSeconds(20));
        boolean staleClaimed = repository().claim(claimed.getIdentifier(), ELIGIBLE, T0.plusSeconds(11),
                T0.plusSeconds(20));
        boolean staleProcessing = repository().claim(processing.getIdentifier(), ELIGIBLE, T0.plusSeconds(11),
                T0.plusSeconds(20));

        // assert
        assertThat(freshClaimed).isFalse();
        assertThat(staleClaimed).isTrue();
        assertThat(staleProcessing).isTrue();
        assertThat(repository().findIncompletePublications()).extracting(p -> p.getLastResubmissionDate().orElseThrow())
                .containsExactly(T0.plusSeconds(20), T0.plusSeconds(20));
    }

    @Test
    void claimsOnlyTheEligibleStatuses() {
        // arrange
        assumeStatus();
        TargetEventPublication published = create("published", LISTENER, 0);

        // act
        boolean claimed = repository().claim(published.getIdentifier(), Set.of(Status.FAILED), T0, T0.plusSeconds(10));

        // assert
        assertThat(claimed).isFalse();
        assertThat(single(repository().findIncompletePublications()).getStatus()).isEqualTo(Status.PUBLISHED);
    }

    @Test
    void readsTheIncompletePublicationsOfOneListenerByStatusPageByPage() {
        // arrange
        assumeStatus();
        create("published", LISTENER, 0);
        TargetEventPublication failed = create("failed", LISTENER, 1);
        TargetEventPublication processing = create("processing", LISTENER, 2);
        TargetEventPublication completed = create("completed", LISTENER, 3);
        create("other", OTHER_LISTENER, 4);
        create("late", LISTENER, 5);
        repository().markFailed(failed.getIdentifier());
        repository().markProcessing(processing.getIdentifier());
        repository().markCompleted(completed.getIdentifier(), T0.plusSeconds(6));
        PageCriteria first = PageCriteria.first(T0.plusSeconds(60), 2);

        // act
        List<TargetEventPublication> page1 = repository().findIncompletePublications(LISTENER, ELIGIBLE, first);
        List<TargetEventPublication> page2 = repository().findIncompletePublications(LISTENER, ELIGIBLE,
                first.next(page1.get(1)));
        List<TargetEventPublication> failedOnly = repository().findIncompletePublications(LISTENER,
                Set.of(Status.FAILED), first);

        // assert
        assertThat(events(page1)).containsExactly("published", "failed");
        assertThat(events(page2)).containsExactly("late");
        assertThat(events(failedOnly)).containsExactly("failed");
    }

    @Test
    void readsTheIncompletePublicationsOfOneListenerInEveryPendingStatus() {
        // arrange
        assumeStatus();
        create("published", LISTENER, 0);
        TargetEventPublication failed = create("failed", LISTENER, 1);
        TargetEventPublication claimed = create("claimed", LISTENER, 2);
        TargetEventPublication processing = create("processing", LISTENER, 3);
        TargetEventPublication completed = create("completed", LISTENER, 4);
        create("other", OTHER_LISTENER, 5);
        repository().markFailed(failed.getIdentifier());
        repository().claim(claimed.getIdentifier(), ELIGIBLE, T0, T0.plusSeconds(10));
        repository().markProcessing(processing.getIdentifier());
        repository().markCompleted(completed.getIdentifier(), T0.plusSeconds(6));
        PageCriteria first = PageCriteria.first(T0.plusSeconds(60), 10);

        // act
        List<TargetEventPublication> pending = repository().findIncompletePublications(LISTENER,
                Set.of(Status.PUBLISHED, Status.FAILED, Status.RESUBMITTED, Status.PROCESSING), first);
        List<TargetEventPublication> inFlight = repository().findIncompletePublications(LISTENER,
                Set.of(Status.RESUBMITTED, Status.PROCESSING), first);

        // assert
        assertThat(events(pending)).containsExactly("published", "failed", "claimed", "processing");
        assertThat(events(inFlight)).containsExactly("claimed", "processing");
    }

    @Test
    void readsPagesWithoutUpperBound() {
        // arrange
        create("early", LISTENER, 0);
        create("late", LISTENER, 10);
        TargetEventPublication done = create("done", LISTENER, 5);
        repository().markCompleted(done.getIdentifier(), T0.plusSeconds(20));

        // act
        List<TargetEventPublication> unbounded = repository().findIncompletePublications(PageCriteria.first(Instant.MAX,
                10));
        List<TargetEventPublication> farFuture = repository().findIncompletePublications(
                PageCriteria.first(EventPublicationRepository.FAR_FUTURE, 10));
        List<TargetEventPublication> listenerPage = repository().findIncompletePublications(LISTENER, ELIGIBLE,
                PageCriteria.first(Instant.MAX, 10));
        List<TargetEventPublication> completedPage = repository().findCompletedPublications(
                PageCriteria.first(Instant.MAX, 10));
        List<TargetEventPublication> failed = repository().findFailedPublications(FailedCriteria.all());

        // assert
        assertThat(events(unbounded)).containsExactly("early", "late");
        assertThat(events(farFuture)).containsExactly("early", "late");
        assertThat(events(listenerPage)).containsExactly("early", "late");
        assertThat(events(completedPage)).containsExactly("done");
        assertThat(failed).isEmpty();
    }

    @Test
    void listsIncompletePublicationsOldestFirstAndByPublicationDate() {
        // arrange
        create("late", LISTENER, 20);
        create("early", LISTENER, 0);
        create("middle", LISTENER, 10);
        TargetEventPublication done = create("done", LISTENER, 5);
        repository().markCompleted(done.getIdentifier(), T0.plusSeconds(30));

        // act
        List<TargetEventPublication> all = repository().findIncompletePublications();
        List<TargetEventPublication> beforeTen = repository().findIncompletePublicationsPublishedBefore(T0.plusSeconds(10));
        List<TargetEventPublication> beforeEleven = repository()
                .findIncompletePublicationsPublishedBefore(T0.plusSeconds(11));

        // assert
        assertThat(events(all)).containsExactly("early", "middle", "late");
        assertThat(events(beforeTen)).containsExactly("early");
        assertThat(events(beforeEleven)).containsExactly("early", "middle");
    }

    @Test
    void findsTheIncompletePublicationOfAnEventForAListener() {
        // arrange
        TargetEventPublication billing = create("e-1", LISTENER, 0);
        create("e-1", OTHER_LISTENER, 0);
        TargetEventPublication done = create("e-2", LISTENER, 1);
        repository().markCompleted(done.getIdentifier(), T0.plusSeconds(2));

        // act
        var open = repository().findIncompletePublicationsByEventAndTargetIdentifier(new TckEvent("e-1"), LISTENER);
        var completed = repository().findIncompletePublicationsByEventAndTargetIdentifier(new TckEvent("e-2"), LISTENER);
        var unknown = repository().findIncompletePublicationsByEventAndTargetIdentifier(new TckEvent("e-3"), LISTENER);
        var otherListener = repository().findIncompletePublicationsByEventAndTargetIdentifier(new TckEvent("e-1"),
                PublicationTargetIdentifier.of("nobody"));

        // assert
        assertThat(open).map(EventPublication::getIdentifier).hasValue(billing.getIdentifier());
        assertThat(completed).isEmpty();
        assertThat(unknown).isEmpty();
        assertThat(otherListener).isEmpty();
    }

    @Test
    void findsFailedPublicationsByCriteria() {
        // arrange
        assumeStatus();
        TargetEventPublication first = create("first", LISTENER, 0);
        TargetEventPublication second = create("second", LISTENER, 10);
        TargetEventPublication third = create("third", LISTENER, 20);
        create("published", LISTENER, 5);
        List.of(third, first, second).forEach(p -> repository().markFailed(p.getIdentifier()));

        // act
        List<TargetEventPublication> all = repository().findFailedPublications(FailedCriteria.all());
        List<TargetEventPublication> two = repository().findFailedPublications(new FailedCriteria(2, Instant.MAX));
        List<TargetEventPublication> beforeTen = repository()
                .findFailedPublications(new FailedCriteria(10, T0.plusSeconds(10)));

        // assert
        assertThat(events(all)).containsExactly("first", "second", "third");
        assertThat(events(two)).containsExactly("first", "second");
        assertThat(events(beforeTen)).containsExactly("first");
    }

    @Test
    void findsAndCountsByStatus() {
        // arrange
        assumeStatus();
        TargetEventPublication failed = create("failed", LISTENER, 0);
        create("published-1", LISTENER, 1);
        create("published-2", LISTENER, 2);
        repository().markFailed(failed.getIdentifier());

        // act
        List<TargetEventPublication> published = repository().findByStatus(Status.PUBLISHED);
        int publishedCount = repository().countByStatus(Status.PUBLISHED);
        int failedCount = repository().countByStatus(Status.FAILED);
        int completedCount = repository().countByStatus(Status.COMPLETED);

        // assert
        assertThat(events(published)).containsExactly("published-1", "published-2");
        assertThat(publishedCount).isEqualTo(2);
        assertThat(failedCount).isEqualTo(1);
        assertThat(completedCount).isZero();
    }

    @Test
    void deletesPublicationsByIdentifier() {
        // arrange
        TargetEventPublication incomplete = create("incomplete", LISTENER, 0);
        TargetEventPublication completed = create("completed", LISTENER, 1);
        create("kept", LISTENER, 2);
        repository().markCompleted(completed.getIdentifier(), T0.plusSeconds(3));

        // act
        repository().deletePublications(List.of(incomplete.getIdentifier(), completed.getIdentifier(), UUID.randomUUID()));

        // assert
        assertThat(events(repository().findIncompletePublications())).containsExactly("kept");
        assertThat(repository().findCompletedPublications()).isEmpty();
    }

    @Test
    void deletesCompletedPublications() {
        // arrange
        TargetEventPublication completed = create("completed", LISTENER, 0);
        create("incomplete", LISTENER, 1);
        repository().markCompleted(completed.getIdentifier(), T0.plusSeconds(2));

        // act
        repository().deleteCompletedPublications();

        // assert
        assertThat(repository().findCompletedPublications()).isEmpty();
        assertThat(events(repository().findIncompletePublications())).containsExactly("incomplete");
    }

    @Test
    void deletesCompletedPublicationsCompletedBeforeAnInstant() {
        // arrange
        TargetEventPublication old = create("old", LISTENER, 0);
        TargetEventPublication recent = create("recent", LISTENER, 1);
        TargetEventPublication boundary = create("boundary", LISTENER, 2);
        create("incomplete", LISTENER, 3);
        repository().markCompleted(old.getIdentifier(), T0.plusSeconds(10));
        repository().markCompleted(boundary.getIdentifier(), T0.plusSeconds(20));
        repository().markCompleted(recent.getIdentifier(), T0.plusSeconds(30));

        // act
        repository().deleteCompletedPublicationsBefore(T0.plusSeconds(20));

        // assert
        assertThat(events(repository().findCompletedPublications())).containsExactly("recent", "boundary");
        assertThat(events(repository().findIncompletePublications())).containsExactly("incomplete");
    }

    @Test
    void findsTheIncompletePublicationOfAnEventAmongManyOfTheListener() {
        // arrange
        for (int i = 0; i < 150; i++) {
            create("bulk-" + i, LISTENER, i);
        }

        // act
        var found = repository().findIncompletePublicationsByEventAndTargetIdentifier(new TckEvent("bulk-149"),
                LISTENER);

        // assert
        assertThat(found).map(p -> ((TckEvent) p.getEvent()).id()).hasValue("bulk-149");
    }

    @Test
    void countsThePublicationsItDeletesByIdentifier() {
        // arrange
        TargetEventPublication incomplete = create("incomplete", LISTENER, 0);
        TargetEventPublication completed = create("completed", LISTENER, 1);
        create("kept", LISTENER, 2);
        repository().markCompleted(completed.getIdentifier(), T0.plusSeconds(3));

        // act
        int deleted = repository().deletePublicationsCounting(
                List.of(incomplete.getIdentifier(), completed.getIdentifier(), UUID.randomUUID()));

        // assert
        assertThat(deleted).isEqualTo(2);
        assertThat(events(repository().findIncompletePublications())).containsExactly("kept");
        assertThat(repository().findCompletedPublications()).isEmpty();
    }

    @Test
    void readsIncompletePublicationsPageByPage() {
        // arrange
        for (int i = 0; i < 5; i++) {
            create("e-" + i, LISTENER, i);
        }
        TargetEventPublication done = create("done", LISTENER, 2);
        repository().markCompleted(done.getIdentifier(), T0.plusSeconds(9));
        PageCriteria first = PageCriteria.first(T0.plusSeconds(4), 2);

        // act
        List<TargetEventPublication> page1 = repository().findIncompletePublications(first);
        List<TargetEventPublication> page2 = repository().findIncompletePublications(first.next(page1.get(1)));
        List<TargetEventPublication> page3 = repository().findIncompletePublications(first.next(page2.get(1)));

        // assert
        assertThat(events(page1)).containsExactly("e-0", "e-1");
        assertThat(events(page2)).containsExactly("e-2", "e-3");
        assertThat(page3).isEmpty();
    }

    @Test
    void pagesPublicationsWithTheSameDateWithoutLosingAny() {
        // arrange
        for (int i = 0; i < 5; i++) {
            create("same-" + i, LISTENER, 0);
        }
        PageCriteria first = PageCriteria.first(T0.plusSeconds(1), 2);

        // act
        List<TargetEventPublication> page1 = repository().findIncompletePublications(first);
        List<TargetEventPublication> page2 = repository().findIncompletePublications(first.next(page1.get(1)));
        List<TargetEventPublication> page3 = repository().findIncompletePublications(first.next(page2.get(1)));

        // assert
        assertThat(page1).hasSize(2);
        assertThat(page2).hasSize(2);
        assertThat(page3).hasSize(1);
        assertThat(events(List.of(page1.get(0), page1.get(1), page2.get(0), page2.get(1), page3.get(0))))
                .containsExactlyInAnyOrder("same-0", "same-1", "same-2", "same-3", "same-4");
    }

    @Test
    void readsCompletedPublicationsPageByPage() {
        // arrange
        for (int i = 0; i < 3; i++) {
            TargetEventPublication publication = create("c-" + i, LISTENER, i);
            repository().markCompleted(publication.getIdentifier(), T0.plusSeconds(10 + i));
        }
        create("open", LISTENER, 1);
        PageCriteria first = PageCriteria.first(T0.plusSeconds(60), 2);

        // act
        List<TargetEventPublication> page1 = repository().findCompletedPublications(first);
        List<TargetEventPublication> page2 = repository().findCompletedPublications(first.next(page1.get(1)));

        // assert
        assertThat(events(page1)).containsExactly("c-0", "c-1");
        assertThat(events(page2)).containsExactly("c-2");
    }

    @Test
    void marksStalePublicationsOfAStatusFailedByTheirLastPublicationDate() {
        // arrange
        assumeStatus();
        create("old", LISTENER, 0);
        TargetEventPublication resubmittedRecently = create("resubmitted", LISTENER, 1);
        create("fresh", LISTENER, 20);
        TargetEventPublication processing = create("processing", LISTENER, 0);
        repository().markFailed(resubmittedRecently.getIdentifier());
        repository().markResubmitted(resubmittedRecently.getIdentifier(), T0.plusSeconds(30));
        repository().markProcessing(processing.getIdentifier());

        // act
        int published = repository().markStalePublicationsFailed(Status.PUBLISHED, T0.plusSeconds(10));
        int resubmitted = repository().markStalePublicationsFailed(Status.RESUBMITTED, T0.plusSeconds(10));

        // assert
        assertThat(published).isEqualTo(1);
        assertThat(resubmitted).isZero();
        assertThat(events(repository().findByStatus(Status.FAILED))).containsExactly("old");
        assertThat(events(repository().findByStatus(Status.PUBLISHED))).containsExactly("fresh");
        assertThat(events(repository().findByStatus(Status.PROCESSING))).containsExactly("processing");
    }

    @Test
    void marksStaleProcessingPublicationsFailedWithoutCountingAnAttempt() {
        // arrange
        assumeStatus();
        TargetEventPublication stuck = create("stuck", LISTENER, 0);
        TargetEventPublication running = create("running", LISTENER, 20);
        repository().markProcessing(stuck.getIdentifier());
        repository().markProcessing(running.getIdentifier());

        // act
        int marked = repository().markStalePublicationsFailed(Status.PROCESSING, T0.plusSeconds(10));

        // assert
        assertThat(marked).isEqualTo(1);
        assertThat(single(repository().findByStatus(Status.FAILED))).satisfies(p -> {
            assertThat(((TckEvent) p.getEvent()).id()).isEqualTo("stuck");
            assertThat(p.getCompletionAttempts()).isEqualTo(1);
        });
        assertThat(events(repository().findByStatus(Status.PROCESSING))).containsExactly("running");
    }

    @Test
    void countsTheCompletedPublicationsItDeletes() {
        // arrange
        TargetEventPublication old = create("old", LISTENER, 0);
        TargetEventPublication recent = create("recent", LISTENER, 1);
        create("incomplete", LISTENER, 2);
        repository().markCompleted(old.getIdentifier(), T0.plusSeconds(10));
        repository().markCompleted(recent.getIdentifier(), T0.plusSeconds(30));

        // act
        int deleted = repository().deleteCompletedPublicationsCompletedBefore(T0.plusSeconds(20));

        // assert
        assertThat(deleted).isEqualTo(1);
        assertThat(events(repository().findCompletedPublications())).containsExactly("recent");
        assertThat(events(repository().findIncompletePublications())).containsExactly("incomplete");
    }

    protected static TargetEventPublication single(List<TargetEventPublication> publications) {
        return Publications.single(publications);
    }

    protected static List<String> events(List<TargetEventPublication> publications) {
        return Publications.events(publications);
    }
}
