package io.quarkiverse.modulith.events.jdbc.runtime;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.StoredPublication;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * The statements both table structures share, after Spring Modulith's {@code JdbcEventPublicationRepository} and
 * {@code JdbcEventPublicationRepositoryV2}. Filters, ordering and limits run in the database. Pages are ordered by
 * publication date, then identifier, in the database's order of both columns.
 */
public abstract class AbstractJdbcEventPublicationRepository implements EventPublicationRepository {

    static final int DELETE_BATCH_SIZE = 100;

    static final String PAGE_ORDER = " ORDER BY PUBLICATION_DATE ASC, ID ASC";

    final JdbcStore store;
    final JdbcOperations operations;
    final JdbcRepositorySettings settings;
    final String columns;

    private final String sqlFindIncomplete;
    private final String sqlFindIncompleteBefore;
    private final String sqlFindByEventAndListener;
    private final String sqlFindCompleted;
    private final String sqlComplete;
    private final String sqlArchive;
    private final String sqlDeleteById;
    private final String sqlDeleteCompleted;
    private final String sqlDeleteCompletedBefore;

    /**
     * @param columns the columns every select reads
     * @param completeStatement the update that completes an incomplete publication, with the completion date and the
     *        identifier as arguments and {@code %s} for the table
     * @param archiveStatement the insert that copies a publication to the archive, with the completion date and the
     *        identifier as arguments and {@code %1$s} for the archive table and {@code %2$s} for the table
     */
    protected AbstractJdbcEventPublicationRepository(JdbcStore store, String columns, String completeStatement,
            String archiveStatement) {
        this.store = store;
        this.operations = store.operations();
        this.settings = store.settings();
        this.columns = columns;
        String table = settings.table();
        String completedTable = settings.completedTable();
        this.sqlFindIncomplete = select(table) + " WHERE COMPLETION_DATE IS NULL ORDER BY PUBLICATION_DATE ASC";
        this.sqlFindIncompleteBefore = select(table)
                + " WHERE COMPLETION_DATE IS NULL AND PUBLICATION_DATE < ? ORDER BY PUBLICATION_DATE ASC";
        this.sqlFindByEventAndListener = select(table)
                + " WHERE SERIALIZED_EVENT = ? AND LISTENER_ID = ? AND COMPLETION_DATE IS NULL ORDER BY PUBLICATION_DATE ASC"
                + settings.databaseType().limitClause(1);
        this.sqlFindCompleted = select(completedTable)
                + " WHERE COMPLETION_DATE IS NOT NULL ORDER BY PUBLICATION_DATE ASC";
        this.sqlComplete = completeStatement.formatted(table);
        this.sqlArchive = archiveStatement.formatted(settings.archiveTable(), table);
        this.sqlDeleteById = "DELETE FROM " + table + " WHERE ID = ?";
        this.sqlDeleteCompleted = "DELETE FROM " + completedTable + " WHERE COMPLETION_DATE IS NOT NULL";
        this.sqlDeleteCompletedBefore = "DELETE FROM " + completedTable + " WHERE COMPLETION_DATE < ?";
    }

    @Override
    public void markCompleted(UUID identifier, Instant completionDate) {
        Object id = id(identifier);
        CompletionMode completion = settings.completion();
        if (completion == CompletionMode.DELETE) {
            operations.update(sqlDeleteById, id);
        } else if (completion == CompletionMode.ARCHIVE) {
            // the update locks the row, so a concurrent completion waits, then finds nothing left to archive
            if (operations.update(sqlComplete, completionDate, id) == 1) {
                operations.update(sqlArchive, completionDate, id);
                operations.update(sqlDeleteById, id);
            }
        } else {
            operations.update(sqlComplete, completionDate, id);
        }
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications() {
        return operations.query(sqlFindIncomplete, this::map);
    }

    @Override
    public List<TargetEventPublication> findIncompletePublicationsPublishedBefore(Instant instant) {
        return operations.query(sqlFindIncompleteBefore, this::map, instant);
    }

    @Override
    public Optional<TargetEventPublication> findIncompletePublicationsByEventAndTargetIdentifier(Object event,
            PublicationTargetIdentifier target) {
        return operations.query(sqlFindByEventAndListener, this::map, serialize(event), target.value()).stream()
                .findFirst();
    }

    @Override
    public List<TargetEventPublication> findCompletedPublications() {
        return operations.query(sqlFindCompleted, this::map);
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications(PageCriteria criteria) {
        return page(settings.table(), "COMPLETION_DATE IS NULL", List.of(), criteria);
    }

    @Override
    public List<TargetEventPublication> findCompletedPublications(PageCriteria criteria) {
        return page(settings.completedTable(), "COMPLETION_DATE IS NOT NULL", List.of(), criteria);
    }

    @Override
    public void deletePublications(List<UUID> identifiers) {
        deletePublicationsCounting(identifiers);
    }

    @Override
    public int deletePublicationsCounting(List<UUID> identifiers) {
        List<String> tables = settings.isArchive() ? List.of(settings.table(), settings.archiveTable())
                : List.of(settings.table());
        int deleted = 0;
        for (int from = 0; from < identifiers.size(); from += DELETE_BATCH_SIZE) {
            Object[] batch = identifiers.subList(from, Math.min(from + DELETE_BATCH_SIZE, identifiers.size())).stream()
                    .map(this::id)
                    .toArray();
            for (String table : tables) {
                deleted += operations.update("DELETE FROM " + table + " WHERE ID IN (" + placeholders(batch.length) + ")",
                        batch);
            }
        }
        return deleted;
    }

    @Override
    public void deleteCompletedPublications() {
        operations.update(sqlDeleteCompleted);
    }

    @Override
    public void deleteCompletedPublicationsBefore(Instant instant) {
        deleteCompletedPublicationsCompletedBefore(instant);
    }

    @Override
    public int deleteCompletedPublicationsCompletedBefore(Instant instant) {
        return operations.update(sqlDeleteCompletedBefore, instant);
    }

    /**
     * @return the status of the current row
     */
    abstract Status status(ResultSet resultSet, Optional<Instant> completionDate) throws SQLException;

    /**
     * @return the completion attempts of the current row
     */
    abstract int completionAttempts(ResultSet resultSet) throws SQLException;

    /**
     * @return the last resubmission date of the current row
     */
    abstract Optional<Instant> lastResubmissionDate(ResultSet resultSet) throws SQLException;

    /**
     * Reads one page: the rows matching the condition in the criteria's range, after its cursor, at most its size.
     *
     * @param arguments the arguments of the condition's placeholders
     */
    List<TargetEventPublication> page(String table, String condition, List<Object> arguments, PageCriteria criteria) {
        StringBuilder sql = new StringBuilder(select(table)).append(" WHERE ").append(condition);
        List<Object> values = new ArrayList<>(arguments);
        if (criteria.publishedBefore().isBefore(FAR_FUTURE)) {
            sql.append(" AND PUBLICATION_DATE < ?");
            values.add(criteria.publishedBefore());
        }
        criteria.after().ifPresent(cursor -> {
            sql.append(" AND (PUBLICATION_DATE > ? OR (PUBLICATION_DATE = ? AND ID > ?))");
            values.add(cursor.publicationDate());
            values.add(cursor.publicationDate());
            values.add(id(cursor.identifier()));
        });
        sql.append(PAGE_ORDER).append(settings.databaseType().limitClause(criteria.maxItems()));
        return operations.query(sql.toString(), this::map, values.toArray());
    }

    String select(String table) {
        return "SELECT " + columns + " FROM " + table;
    }

    Object id(UUID identifier) {
        return settings.databaseType().uuidToDatabase(identifier);
    }

    String serialize(Object event) {
        return store.serializer().serialize(event).toString();
    }

    static String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }

    Optional<TargetEventPublication> map(ResultSet resultSet) throws SQLException {
        UUID id = DatabaseType.databaseToUuid(resultSet.getObject("ID"));
        Optional<Instant> completionDate = instant(resultSet.getTimestamp("COMPLETION_DATE"));
        return Optional.of(new StoredPublication(id, resultSet.getString("EVENT_TYPE"),
                resultSet.getString("SERIALIZED_EVENT"), store.serializer(),
                PublicationTargetIdentifier.of(resultSet.getString("LISTENER_ID")),
                resultSet.getTimestamp("PUBLICATION_DATE").toInstant(), completionDate,
                status(resultSet, completionDate), completionAttempts(resultSet), lastResubmissionDate(resultSet)));
    }

    static Optional<Instant> instant(Timestamp timestamp) {
        return Optional.ofNullable(timestamp).map(Timestamp::toInstant);
    }
}
