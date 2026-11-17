package io.quarkiverse.modulith.events.jdbc.runtime;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.core.FailedCriteria;
import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * The JDBC event publication repository for the legacy table structure (Spring Modulith's version 1 schema, selected
 * with {@code quarkus.modulith.events.jdbc.use-legacy-structure=true}). The table has no status, completion attempts or
 * resubmission date, so, as in Spring Modulith, incomplete publications read as {@code PUBLISHED}, failures are not
 * recorded and no publication is found as failed. A claim only checks that the publication is incomplete, so two
 * instances can claim the same publication: use the version 2 structure with several instances. Each call joins the
 * caller's transaction or runs in its own.
 */
@Singleton
@Transactional
public class LegacyJdbcEventPublicationRepository extends AbstractJdbcEventPublicationRepository {

    static final String COLUMNS = "ID, COMPLETION_DATE, EVENT_TYPE, LISTENER_ID, PUBLICATION_DATE, SERIALIZED_EVENT";

    private static final String COMPLETE = "UPDATE %s SET COMPLETION_DATE = ? WHERE ID = ? AND COMPLETION_DATE IS NULL";

    private static final String ARCHIVE = "INSERT INTO %1$s (ID, LISTENER_ID, EVENT_TYPE, SERIALIZED_EVENT, "
            + "PUBLICATION_DATE, COMPLETION_DATE) "
            + "SELECT P.ID, P.LISTENER_ID, P.EVENT_TYPE, P.SERIALIZED_EVENT, P.PUBLICATION_DATE, ? FROM %2$s P "
            + "WHERE P.ID = ? AND NOT EXISTS (SELECT 1 FROM %1$s A WHERE A.ID = P.ID)";

    private final String sqlInsert;
    private final String sqlCountIncomplete;

    @Inject
    public LegacyJdbcEventPublicationRepository(JdbcStore store) {
        super(store, COLUMNS, COMPLETE, ARCHIVE);
        String table = settings.table();
        this.sqlInsert = "INSERT INTO " + table + " (ID, EVENT_TYPE, LISTENER_ID, PUBLICATION_DATE, SERIALIZED_EVENT, "
                + "COMPLETION_DATE) VALUES (?, ?, ?, ?, ?, ?)";
        this.sqlCountIncomplete = "SELECT COUNT(*) FROM " + table + " WHERE ID = ? AND COMPLETION_DATE IS NULL";
    }

    @Override
    public TargetEventPublication create(TargetEventPublication publication) {
        operations.update(sqlInsert, id(publication.getIdentifier()), publication.getEventType(),
                publication.getTargetIdentifier().value(), publication.getPublicationDate(),
                serialize(publication.getEvent()), publication.getCompletionDate());
        return publication;
    }

    /**
     * @return whether the publication is incomplete. The attempt is not counted, there is no column for it.
     */
    @Override
    public boolean markProcessing(UUID identifier) {
        return isIncomplete(identifier);
    }

    /**
     * Does nothing, there is no column for the status.
     */
    @Override
    public void markFailed(UUID identifier) {
        // the legacy structure has no status column
    }

    /**
     * @return whether the publication is incomplete. Without a status column a second claim cannot be told apart, so
     *         several instances may resubmit the same publication.
     */
    @Override
    public boolean markResubmitted(UUID identifier, Instant resubmissionDate) {
        return isIncomplete(identifier);
    }

    /**
     * @return whether the publication is incomplete and published publications are eligible. Not atomic: without a
     *         status column a claim cannot be told apart from a delivery.
     */
    @Override
    public boolean claim(UUID identifier, Set<Status> eligible, Instant staleBefore, Instant claimDate) {
        return eligible.contains(Status.PUBLISHED) && isIncomplete(identifier);
    }

    /**
     * @return the listener's incomplete publications, which all read as published, if published ones are asked for
     */
    @Override
    public List<TargetEventPublication> findIncompletePublications(PublicationTargetIdentifier listener,
            Set<Status> statuses, PageCriteria criteria) {
        if (!statuses.contains(Status.PUBLISHED)) {
            return List.of();
        }
        return page(settings.table(), "COMPLETION_DATE IS NULL AND LISTENER_ID = ?", List.of(listener.value()),
                criteria);
    }

    /**
     * @return zero, failures are not recorded in the legacy structure
     */
    @Override
    public int markStalePublicationsFailed(Status status, Instant lastPublishedBefore) {
        return 0;
    }

    /**
     * @return no publications, failures are not recorded in the legacy structure
     */
    @Override
    public List<TargetEventPublication> findFailedPublications(FailedCriteria criteria) {
        return List.of();
    }

    @Override
    public List<TargetEventPublication> findByStatus(Status status) {
        if (status == Status.COMPLETED) {
            return findCompletedPublications();
        }
        return status == Status.PUBLISHED ? findIncompletePublications() : List.of();
    }

    @Override
    public int countByStatus(Status status) {
        if (status == Status.COMPLETED) {
            return operations.count("SELECT COUNT(*) FROM " + settings.completedTable()
                    + " WHERE COMPLETION_DATE IS NOT NULL");
        }
        return status == Status.PUBLISHED
                ? operations.count("SELECT COUNT(*) FROM " + settings.table() + " WHERE COMPLETION_DATE IS NULL")
                : 0;
    }

    @Override
    Status status(ResultSet resultSet, Optional<Instant> completionDate) {
        return completionDate.isPresent() ? Status.COMPLETED : Status.PUBLISHED;
    }

    @Override
    int completionAttempts(ResultSet resultSet) {
        return 0;
    }

    @Override
    Optional<Instant> lastResubmissionDate(ResultSet resultSet) {
        return Optional.empty();
    }

    private boolean isIncomplete(UUID identifier) {
        return operations.count(sqlCountIncomplete, id(identifier)) > 0;
    }
}
