package io.quarkiverse.modulith.events.jdbc.runtime;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
 * The JDBC event publication repository for the current table structure (Spring Modulith's version 2 schema, with
 * status, completion attempts and last resubmission date). Each call joins the caller's transaction or runs in its
 * own. A row without status, kept from a version 1 table or written by Spring Modulith 1.x, has the status
 * {@code COMPLETED} when it has a completion date and {@code FAILED} otherwise, in what it reads as and in every
 * filter on the status.
 */
@Singleton
@Transactional
public class JdbcEventPublicationRepository extends AbstractJdbcEventPublicationRepository {

    static final String COLUMNS = "ID, COMPLETION_DATE, EVENT_TYPE, LISTENER_ID, PUBLICATION_DATE, SERIALIZED_EVENT, "
            + "STATUS, COMPLETION_ATTEMPTS, LAST_RESUBMISSION_DATE";

    private static final String STALE_CLAIM = " OR (STATUS IN ('RESUBMITTED', 'PROCESSING') "
            + "AND COALESCE(LAST_RESUBMISSION_DATE, PUBLICATION_DATE) < ?))";

    /**
     * The status of a row, a row without status counting as completed when it has a completion date and as failed
     * otherwise.
     */
    static final String EFFECTIVE_STATUS = "COALESCE(STATUS, CASE WHEN COMPLETION_DATE IS NULL THEN 'FAILED' "
            + "ELSE 'COMPLETED' END)";

    private static final String COMPLETE = "UPDATE %s SET STATUS = 'COMPLETED', COMPLETION_DATE = ? "
            + "WHERE ID = ? AND COMPLETION_DATE IS NULL";

    private static final String ARCHIVE = "INSERT INTO %1$s (ID, LISTENER_ID, EVENT_TYPE, SERIALIZED_EVENT, "
            + "PUBLICATION_DATE, STATUS, COMPLETION_DATE, COMPLETION_ATTEMPTS, LAST_RESUBMISSION_DATE) "
            + "SELECT P.ID, P.LISTENER_ID, P.EVENT_TYPE, P.SERIALIZED_EVENT, P.PUBLICATION_DATE, 'COMPLETED', ?, "
            + "P.COMPLETION_ATTEMPTS, P.LAST_RESUBMISSION_DATE FROM %2$s P "
            + "WHERE P.ID = ? AND NOT EXISTS (SELECT 1 FROM %1$s A WHERE A.ID = P.ID)";

    private final String sqlInsert;
    private final String sqlMarkProcessing;
    private final String sqlMarkFailed;
    private final String sqlMarkResubmitted;
    private final String sqlFindFailed;
    private final String sqlClaim;
    private final String sqlMarkStale;

    @Inject
    public JdbcEventPublicationRepository(JdbcStore store) {
        super(store, COLUMNS, COMPLETE, ARCHIVE);
        String table = settings.table();
        this.sqlInsert = "INSERT INTO " + table + " (ID, EVENT_TYPE, LISTENER_ID, PUBLICATION_DATE, SERIALIZED_EVENT, "
                + "STATUS, COMPLETION_ATTEMPTS, LAST_RESUBMISSION_DATE, COMPLETION_DATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        this.sqlMarkProcessing = "UPDATE " + table + " SET STATUS = 'PROCESSING', "
                + "COMPLETION_ATTEMPTS = COALESCE(COMPLETION_ATTEMPTS, 0) + 1 WHERE ID = ? AND COMPLETION_DATE IS NULL";
        this.sqlMarkFailed = "UPDATE " + table + " SET STATUS = 'FAILED' WHERE ID = ? AND COMPLETION_DATE IS NULL";
        this.sqlMarkResubmitted = "UPDATE " + table + " SET STATUS = 'RESUBMITTED', LAST_RESUBMISSION_DATE = ? "
                + "WHERE ID = ? AND COMPLETION_DATE IS NULL AND (STATUS IS NULL OR STATUS <> 'RESUBMITTED')";
        this.sqlClaim = "UPDATE " + table + " SET STATUS = 'RESUBMITTED', LAST_RESUBMISSION_DATE = ? "
                + "WHERE ID = ? AND COMPLETION_DATE IS NULL AND (";
        this.sqlMarkStale = "UPDATE " + table + " SET STATUS = 'FAILED' WHERE STATUS = ? AND COMPLETION_DATE IS NULL "
                + "AND COALESCE(LAST_RESUBMISSION_DATE, PUBLICATION_DATE) < ?";
        this.sqlFindFailed = select(table) + " WHERE COMPLETION_DATE IS NULL AND " + EFFECTIVE_STATUS + " = 'FAILED'";
    }

    @Override
    public TargetEventPublication create(TargetEventPublication publication) {
        operations.update(sqlInsert, id(publication.getIdentifier()), publication.getEventType(),
                publication.getTargetIdentifier().value(), publication.getPublicationDate(),
                serialize(publication.getEvent()), publication.getStatus().name(), publication.getCompletionAttempts(),
                publication.getLastResubmissionDate(), publication.getCompletionDate());
        return publication;
    }

    @Override
    public boolean markProcessing(UUID identifier) {
        return operations.update(sqlMarkProcessing, id(identifier)) == 1;
    }

    @Override
    public void markFailed(UUID identifier) {
        operations.update(sqlMarkFailed, id(identifier));
    }

    @Override
    public boolean markResubmitted(UUID identifier, Instant resubmissionDate) {
        return operations.update(sqlMarkResubmitted, resubmissionDate, id(identifier)) == 1;
    }

    /**
     * One conditional update, so two instances never both claim a publication.
     */
    @Override
    public boolean claim(UUID identifier, Set<Status> eligible, Instant staleBefore, Instant claimDate) {
        List<Object> arguments = new ArrayList<>(List.of(claimDate, id(identifier)));
        String eligibleCondition = statusIn(eligible, arguments);
        arguments.add(staleBefore);
        return operations.update(sqlClaim + eligibleCondition + STALE_CLAIM, arguments.toArray()) == 1;
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications(PublicationTargetIdentifier listener,
            Set<Status> statuses, PageCriteria criteria) {
        List<Object> arguments = new ArrayList<>(List.of(listener.value()));
        String condition = "COMPLETION_DATE IS NULL AND LISTENER_ID = ? AND " + statusIn(statuses, arguments);
        return page(settings.table(), condition, arguments, criteria);
    }

    @Override
    public int markStalePublicationsFailed(Status status, Instant lastPublishedBefore) {
        return operations.update(sqlMarkStale, status.name(), lastPublishedBefore);
    }

    @Override
    public List<TargetEventPublication> findFailedPublications(FailedCriteria criteria) {
        StringBuilder sql = new StringBuilder(sqlFindFailed);
        List<Object> arguments = new ArrayList<>();
        if (criteria.publishedBefore().isBefore(FAR_FUTURE)) {
            sql.append(" AND PUBLICATION_DATE < ?");
            arguments.add(criteria.publishedBefore());
        }
        sql.append(" ORDER BY PUBLICATION_DATE ASC");
        if (criteria.maxItems() < Integer.MAX_VALUE) {
            sql.append(settings.databaseType().limitClause(criteria.maxItems()));
        }
        return operations.query(sql.toString(), this::map, arguments.toArray());
    }

    @Override
    public List<TargetEventPublication> findByStatus(Status status) {
        return operations.query(select(tableOf(status)) + " WHERE " + EFFECTIVE_STATUS
                + " = ? ORDER BY PUBLICATION_DATE ASC", this::map, status.name());
    }

    @Override
    public int countByStatus(Status status) {
        return operations.count("SELECT COUNT(*) FROM " + tableOf(status) + " WHERE " + EFFECTIVE_STATUS + " = ?",
                status.name());
    }

    @Override
    Status status(ResultSet resultSet, Optional<Instant> completionDate) throws SQLException {
        String status = resultSet.getString("STATUS");
        if (Objects.nonNull(status)) {
            return Status.valueOf(status);
        }
        return completionDate.isPresent() ? Status.COMPLETED : Status.FAILED;
    }

    @Override
    int completionAttempts(ResultSet resultSet) throws SQLException {
        return resultSet.getInt("COMPLETION_ATTEMPTS");
    }

    @Override
    Optional<Instant> lastResubmissionDate(ResultSet resultSet) throws SQLException {
        return instant(resultSet.getTimestamp("LAST_RESUBMISSION_DATE"));
    }

    /**
     * @return the condition that the row's status is one of the statuses, or a condition that matches nothing for
     *         none
     */
    private static String statusIn(Set<Status> statuses, List<Object> arguments) {
        if (statuses.isEmpty()) {
            return "1 = 0";
        }
        statuses.stream().map(Status::name).sorted().forEach(arguments::add);
        return EFFECTIVE_STATUS + " IN (" + placeholders(statuses.size()) + ")";
    }

    private String tableOf(Status status) {
        return status == Status.COMPLETED ? settings.completedTable() : settings.table();
    }
}
