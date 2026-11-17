package io.quarkiverse.modulith.events.core;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.quarkiverse.modulith.events.EventPublication.Status;

/**
 * Stores event publications. Each persistence extension (JPA, JDBC, MongoDB, Neo4j) provides one CDI bean of this
 * type, and the event publication registry builds on it, like Spring Modulith's {@code EventPublicationRepository}.
 * <p>
 * The contract is defined by the tests in {@code quarkus-modulith-events-tck}, which every implementation must pass.
 * Publications are compared by identifier. Methods are called inside the caller's transaction when there is one.
 * Lists are ordered by publication date, oldest first.
 * <p>
 * Completion attempts count deliveries: {@link #create} stores the attempts of the publication it is given (none for
 * a new publication), every {@link #markProcessing} adds one, and {@link #markResubmitted} adds none. Spring Modulith
 * 2.1.2 counts the other way round (one at creation, one per resubmission, none when processing), so rows it wrote
 * keep their count and new rows count one attempt per delivery. Stores whose table has no status or attempt columns
 * (the legacy JDBC structure) report the status from the completion date and zero attempts.
 * <p>
 * A row without status, written by Spring Modulith 1.x or kept from a version 1 table migrated to the version 2
 * structure, has the status {@code COMPLETED} when it has a completion date and {@code FAILED} otherwise, in what it
 * reads as and in every filter on the status: {@link #findFailedPublications}, {@link #claim},
 * {@link #findByStatus}, {@link #countByStatus} and the paged read of a listener's publications. That is how Spring
 * Modulith 2.1.2 resubmits such a row: its {@code findFailedPublications} returns it and its status updates accept it.
 * The staleness monitor watches {@code PUBLISHED}, {@code PROCESSING} and {@code RESUBMITTED}, so it leaves the row
 * alone, as Spring Modulith's does. Spring Modulith's own {@code getStatus()} reports such a row as
 * {@code PROCESSING} (JDBC) or {@code PUBLISHED} (Neo4j); no behaviour depends on that value there.
 * <p>
 * Serialization goes through the {@link EventSerializer} bean. A store that keeps text calls {@code toString()} on the
 * serialized form, as Spring Modulith's stores do. The default methods are correct for every store but read every
 * matching publication; stores override them to filter and page in the database.
 */
public interface EventPublicationRepository {

    /**
     * Stores a new publication.
     *
     * @return the stored publication
     */
    TargetEventPublication create(TargetEventPublication publication);

    /**
     * Marks the publication as being processed: status {@code PROCESSING}, completion attempts plus one.
     *
     * @return {@code false} if there is no such incomplete publication
     */
    boolean markProcessing(UUID identifier);

    /**
     * Marks the publication as completed: status {@code COMPLETED}, the given completion date. Unknown identifiers are
     * ignored.
     */
    void markCompleted(UUID identifier, Instant completionDate);

    /**
     * Marks the publication as failed: status {@code FAILED}. The attempt count does not change. Unknown and completed
     * publications are left alone, so a late failure of an earlier attempt cannot undo a completion.
     */
    void markFailed(UUID identifier);

    /**
     * Sets an incomplete publication to status {@code RESUBMITTED} with the given resubmission date, like Spring
     * Modulith 2.1.2. The attempt count does not change; the delivery that follows counts in {@link #markProcessing}.
     * The update only excludes publications that are {@code RESUBMITTED} already, so it is not exclusive: it also
     * succeeds on a publication another instance is delivering ({@code PROCESSING}). Use {@link #claim} to take a
     * publication over exclusively; the registry only calls this method through the default {@link #claim}.
     *
     * @return {@code true} if this call changed the publication, {@code false} if it is completed, unknown or already
     *         resubmitted
     */
    boolean markResubmitted(UUID identifier, Instant resubmissionDate);

    /**
     * The upper bound for "every publication", inside the range of every database timestamp type, also after a
     * conversion to any time zone. Stores treat a bound at or after it as no bound, so callers that mean "every
     * publication" pass it rather than {@link Instant#MAX}, which no database stores.
     */
    Instant FAR_FUTURE = Instant.parse("9999-12-30T00:00:00Z");

    /**
     * The statuses of a publication that is claimed or being delivered, which {@link #claim} takes over once stale.
     */
    Set<Status> STALEABLE = Set.of(Status.RESUBMITTED, Status.PROCESSING);

    /**
     * Claims an incomplete publication for a new delivery: status {@code RESUBMITTED}, the claim date as the last
     * resubmission date. The claim succeeds only if the publication's status is one of {@code eligible} (an incomplete
     * row without status counts as {@code FAILED}), or if it is {@code RESUBMITTED} or {@code PROCESSING} with a last
     * publication date (the last resubmission, or else the publication) strictly before {@code staleBefore}, which
     * takes over a delivery whose instance died. A publication another instance claimed or is delivering is
     * therefore never claimed twice, unlike {@link #markResubmitted}, which only excludes {@code RESUBMITTED}.
     * <p>
     * Stores implement this as one conditional update and report whether it changed the row. The default reads the
     * publication first and is not atomic, so it is only safe with a single instance.
     *
     * @param eligible the statuses that may be claimed, usually {@code PUBLISHED} and {@code FAILED}
     * @param staleBefore claimed or processing publications last published before this instant may be taken over
     * @param claimDate the new last resubmission date
     * @return {@code true} if this call claimed the publication
     */
    default boolean claim(UUID identifier, Set<Status> eligible, Instant staleBefore, Instant claimDate) {
        Optional<TargetEventPublication> current = findIncompletePublications().stream()
                .filter(p -> p.getIdentifier().equals(identifier)).findFirst()
                .filter(p -> eligible.contains(p.getStatus())
                        || STALEABLE.contains(p.getStatus()) && p.getLastPublicationDate().isBefore(staleBefore));
        if (current.isEmpty()) {
            return false;
        }
        if (current.get().getStatus() == Status.RESUBMITTED) {
            // markResubmitted refuses a RESUBMITTED row, and this one is stale
            markFailed(identifier);
        }
        return markResubmitted(identifier, claimDate);
    }

    /**
     * @return the publications without completion date
     */
    List<TargetEventPublication> findIncompletePublications();

    /**
     * @return the publications without completion date published strictly before the instant
     */
    List<TargetEventPublication> findIncompletePublicationsPublishedBefore(Instant instant);

    /**
     * @return the incomplete publication of an equal event for the listener, if any
     */
    Optional<TargetEventPublication> findIncompletePublicationsByEventAndTargetIdentifier(Object event,
            PublicationTargetIdentifier target);

    /**
     * @return the publications with a completion date
     */
    List<TargetEventPublication> findCompletedPublications();

    /**
     * @return the publications with status {@code FAILED} matching the criteria, incomplete rows without status
     *         included
     */
    List<TargetEventPublication> findFailedPublications(FailedCriteria criteria);

    /**
     * @return the publications with the given status
     */
    List<TargetEventPublication> findByStatus(Status status);

    /**
     * @return the number of publications with the given status
     */
    int countByStatus(Status status);

    /**
     * Deletes the given publications, completed or not. In {@code archive} completion mode that includes archived
     * publications, unlike Spring Modulith 2.1.2, which only deletes from the publication table. Unknown identifiers
     * are ignored.
     */
    void deletePublications(List<UUID> identifiers);

    /**
     * Deletes the given publications like {@link #deletePublications(List)} and counts what was deleted.
     * <p>
     * Stores must override this with a counting delete in the database. The default reads every incomplete and
     * completed publication for each call, and the registry calls it once per page it deletes.
     *
     * @return the number of publications deleted
     */
    default int deletePublicationsCounting(List<UUID> identifiers) {
        Set<UUID> wanted = Set.copyOf(identifiers);
        int existing = (int) Stream
                .concat(findIncompletePublications().stream(), findCompletedPublications().stream())
                .map(TargetEventPublication::getIdentifier).distinct().filter(wanted::contains).count();
        deletePublications(identifiers);
        return existing;
    }

    /**
     * Deletes all completed publications.
     */
    void deleteCompletedPublications();

    /**
     * Deletes the completed publications whose completion date is strictly before the instant.
     */
    void deleteCompletedPublicationsBefore(Instant instant);

    /**
     * Reads one page of incomplete publications, for resubmissions that must not load the whole backlog.
     *
     * @return the incomplete publications in the criteria's range, ordered by publication date, then identifier
     */
    default List<TargetEventPublication> findIncompletePublications(PageCriteria criteria) {
        return findIncompletePublicationsPublishedBefore(criteria.publishedBefore()).stream()
                .filter(criteria::matches).sorted(PublicationCursor.ORDER).limit(criteria.maxItems()).toList();
    }

    /**
     * Reads one page of the incomplete publications of one listener with one of the given statuses (a row without
     * status counts as {@code FAILED}), for consumers that own a listener's publications, such as the outbox
     * drain of event externalization. Stores filter in the database.
     *
     * @return the matching publications in the criteria's range, ordered by publication date, then identifier
     */
    default List<TargetEventPublication> findIncompletePublications(PublicationTargetIdentifier listener,
            Set<Status> statuses, PageCriteria criteria) {
        return findIncompletePublicationsPublishedBefore(criteria.publishedBefore()).stream()
                .filter(p -> p.getTargetIdentifier().equals(listener) && statuses.contains(p.getStatus()))
                .filter(criteria::matches).sorted(PublicationCursor.ORDER).limit(criteria.maxItems()).toList();
    }

    /**
     * Reads one page of completed publications.
     *
     * @return the completed publications in the criteria's range, ordered by publication date, then identifier
     */
    default List<TargetEventPublication> findCompletedPublications(PageCriteria criteria) {
        return findCompletedPublications().stream().filter(criteria::matches).sorted(PublicationCursor.ORDER)
                .limit(criteria.maxItems()).toList();
    }

    /**
     * Marks the publications with the given status whose last publication date (the last resubmission, or else the
     * publication) is strictly before the instant as failed.
     *
     * @return the number of publications marked failed
     */
    default int markStalePublicationsFailed(Status status, Instant lastPublishedBefore) {
        List<UUID> stale = findByStatus(status).stream()
                .filter(p -> !p.isCompleted() && p.getLastPublicationDate().isBefore(lastPublishedBefore))
                .map(TargetEventPublication::getIdentifier).toList();
        stale.forEach(this::markFailed);
        return stale.size();
    }

    /**
     * Deletes the completed publications whose completion date is strictly before the instant.
     *
     * @return the number of deleted publications
     */
    default int deleteCompletedPublicationsCompletedBefore(Instant instant) {
        int count = (int) findCompletedPublications().stream()
                .filter(p -> p.getCompletionDate().filter(date -> date.isBefore(instant)).isPresent()).count();
        deleteCompletedPublicationsBefore(instant);
        return count;
    }

    /**
     * Runs the deliveries of a publishing transaction once what this store wrote in it is visible. By default that is
     * right after the JTA transaction commits ({@code afterTransactionCommit}). A store that commits its own work
     * after the JTA transaction, for example in a synchronization of its own, overrides this to run the action after
     * that commit, so listeners never look for data that is not there yet.
     *
     * @param action the deliveries
     * @param afterTransactionCommit runs an action after the JTA transaction committed, and drops it on rollback
     */
    default void afterCommit(Runnable action, Consumer<Runnable> afterTransactionCommit) {
        afterTransactionCommit.accept(action);
    }
}
