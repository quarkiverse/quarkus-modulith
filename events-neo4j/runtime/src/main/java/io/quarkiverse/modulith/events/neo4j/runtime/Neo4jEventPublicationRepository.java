package io.quarkiverse.modulith.events.neo4j.runtime;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionCallback;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.FailedCriteria;
import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.StoredPublication;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkus.runtime.BlockingOperationControl;

/**
 * The Neo4j event publication repository, after Spring Modulith's {@code Neo4jEventPublicationRepository}:
 * {@code Neo4jEventPublication} nodes with the same properties, looked up by the hash of the serialized event, and
 * {@code Neo4jEventPublicationArchive} nodes in archive completion mode. Each call runs in its own driver transaction;
 * Neo4j does not take part in JTA transactions. The driver blocks, so calls from an I/O thread are refused.
 * <p>
 * A node without status, written by Spring Modulith 1.x, has the status {@code COMPLETED} when it has a completion date
 * and {@code FAILED} otherwise, in what it reads as and in every filter on the status.
 */
@Singleton
public class Neo4jEventPublicationRepository implements EventPublicationRepository {

    static final String LABEL = "Neo4jEventPublication";
    static final String ARCHIVE_LABEL = "Neo4jEventPublicationArchive";

    private static final String RETURN_OLDEST_FIRST = " RETURN p ORDER BY p.publicationDate ASC";
    private static final String RETURN_PAGE = " RETURN p ORDER BY p.publicationDate ASC, p.identifier ASC LIMIT $limit";

    /**
     * Takes the node's write lock before the condition is read, so a concurrent conditional update sees the value
     * the other transaction committed instead of both succeeding.
     */
    private static final String LOCK = " SET p.identifier = p.identifier WITH p ";

    private static final String LAST_PUBLISHED_BEFORE = "coalesce(p.lastResubmissionDate, p.publicationDate) < $before";

    /**
     * The status of a node, a node without status counting as completed when it has a completion date and as failed
     * otherwise.
     */
    static final String EFFECTIVE_STATUS = "coalesce(p.status, CASE WHEN p.completionDate IS NULL THEN 'FAILED' "
            + "ELSE 'COMPLETED' END)";

    /**
     * Bulk deletes run in transactions of at most this many nodes, so the server never holds them all in one.
     */
    static final int DELETE_BATCH_SIZE = 1000;

    static final String BLOCKING_NOT_ALLOWED = "The Neo4j event publication repository blocks on the driver, call it "
            + "from a worker or virtual thread, not from an I/O thread";

    private final Neo4jStore store;
    private final String completedLabel;
    private final BooleanSupplier blockingAllowed;

    @Inject
    public Neo4jEventPublicationRepository(Neo4jStore store) {
        this(store, BlockingOperationControl::isBlockingAllowed);
    }

    /**
     * For use outside a Quarkus application, where {@link BlockingOperationControl} is not set up.
     *
     * @param blockingAllowed whether the current thread may block
     */
    public Neo4jEventPublicationRepository(Neo4jStore store, BooleanSupplier blockingAllowed) {
        this.store = store;
        this.completedLabel = store.completion() == CompletionMode.ARCHIVE ? ARCHIVE_LABEL : LABEL;
        this.blockingAllowed = blockingAllowed;
    }

    @Override
    public TargetEventPublication create(TargetEventPublication publication) {
        String serialized = serialize(publication.getEvent());
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("identifier", publication.getIdentifier().toString());
        parameters.put("eventSerialized", serialized);
        parameters.put("eventHash", EventHash.sha256(serialized));
        parameters.put("eventType", publication.getEventType());
        parameters.put("listenerId", publication.getTargetIdentifier().value());
        parameters.put("publicationDate", date(publication.getPublicationDate()));
        parameters.put("completionDate", publication.getCompletionDate().map(Neo4jEventPublicationRepository::date)
                .orElse(null));
        parameters.put("status", publication.getStatus().name());
        parameters.put("completionAttempts", publication.getCompletionAttempts());
        parameters.put("lastResubmissionDate", publication.getLastResubmissionDate()
                .map(Neo4jEventPublicationRepository::date).orElse(null));
        write(tx -> tx.run("CREATE (p:" + LABEL + ") SET p = $properties", Map.of("properties", parameters)).consume());
        return publication;
    }

    @Override
    public boolean markProcessing(UUID identifier) {
        return count(true, "MATCH (p:" + LABEL + " {identifier: $identifier}) WHERE p.completionDate IS NULL "
                + "SET p.status = 'PROCESSING', p.completionAttempts = coalesce(p.completionAttempts, 0) + 1 "
                + "RETURN count(p)", Map.of("identifier", identifier.toString())) == 1;
    }

    @Override
    public void markCompleted(UUID identifier, Instant completionDate) {
        Map<String, Object> parameters = Map.of("identifier", identifier.toString(), "completionDate",
                date(completionDate));
        String delete = "MATCH (p:" + LABEL + " {identifier: $identifier}) DELETE p";
        CompletionMode completion = store.completion();
        if (completion == CompletionMode.DELETE) {
            write(tx -> tx.run(delete, parameters).consume());
        } else if (completion == CompletionMode.ARCHIVE) {
            write(tx -> {
                tx.run("MATCH (p:" + LABEL + " {identifier: $identifier})" + LOCK + "WHERE p.completionDate IS NULL "
                        + "AND NOT EXISTS { MATCH (a:" + ARCHIVE_LABEL + " {identifier: $identifier}) } "
                        + "CREATE (a:" + ARCHIVE_LABEL + ") SET a = properties(p), a.completionDate = $completionDate, "
                        + "a.status = 'COMPLETED'", parameters).consume();
                return tx.run(delete, parameters).consume();
            });
        } else {
            write(tx -> tx.run("MATCH (p:" + LABEL + " {identifier: $identifier}) WHERE p.completionDate IS NULL "
                    + "SET p.completionDate = $completionDate, p.status = 'COMPLETED'", parameters).consume());
        }
    }

    @Override
    public void markFailed(UUID identifier) {
        write(tx -> tx.run("MATCH (p:" + LABEL + " {identifier: $identifier}) WHERE p.completionDate IS NULL "
                + "SET p.status = 'FAILED'", Map.of("identifier", identifier.toString())).consume());
    }

    @Override
    public boolean markResubmitted(UUID identifier, Instant resubmissionDate) {
        return count(true, "MATCH (p:" + LABEL + " {identifier: $identifier})" + LOCK + "WHERE p.completionDate IS NULL "
                + "AND (p.status IS NULL OR p.status <> 'RESUBMITTED') "
                + "SET p.status = 'RESUBMITTED', p.lastResubmissionDate = $date RETURN count(p)",
                Map.of("identifier", identifier.toString(), "date", date(resubmissionDate))) == 1;
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications() {
        return read("MATCH (p:" + LABEL + ") WHERE p.completionDate IS NULL" + RETURN_OLDEST_FIRST, Map.of());
    }

    @Override
    public List<TargetEventPublication> findIncompletePublicationsPublishedBefore(Instant instant) {
        return read("MATCH (p:" + LABEL + ") WHERE p.completionDate IS NULL AND p.publicationDate < $date"
                + RETURN_OLDEST_FIRST, Map.of("date", date(instant)));
    }

    @Override
    public Optional<TargetEventPublication> findIncompletePublicationsByEventAndTargetIdentifier(Object event,
            PublicationTargetIdentifier target) {
        return read("MATCH (p:" + LABEL + ") WHERE p.eventHash IN $hashes AND p.listenerId = $listenerId "
                + "AND p.completionDate IS NULL" + RETURN_OLDEST_FIRST + " LIMIT 1",
                Map.of("hashes", EventHash.candidates(serialize(event)), "listenerId", target.value()))
                .stream().findFirst();
    }

    @Override
    public List<TargetEventPublication> findCompletedPublications() {
        return read("MATCH (p:" + completedLabel + ") WHERE p.completionDate IS NOT NULL" + RETURN_OLDEST_FIRST,
                Map.of());
    }

    /**
     * One conditional update under the node's write lock, so two instances never both claim a publication.
     */
    @Override
    public boolean claim(UUID identifier, Set<Status> eligible, Instant staleBefore, Instant claimDate) {
        return count(true, "MATCH (p:" + LABEL + " {identifier: $identifier})" + LOCK + "WHERE p.completionDate IS NULL "
                + "AND (" + EFFECTIVE_STATUS + " IN $eligible OR (p.status IN $staleable AND "
                + LAST_PUBLISHED_BEFORE + ")) SET p.status = 'RESUBMITTED', p.lastResubmissionDate = $date "
                + "RETURN count(p)",
                Map.of("identifier", identifier.toString(), "eligible", names(eligible),
                        "staleable", names(STALEABLE), "before", date(staleBefore), "date", date(claimDate))) == 1;
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications(PageCriteria criteria) {
        return page(LABEL, "p.completionDate IS NULL", Map.of(), criteria);
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications(PublicationTargetIdentifier listener,
            Set<Status> statuses, PageCriteria criteria) {
        return page(LABEL, "p.completionDate IS NULL AND p.listenerId = $listener "
                + "AND " + EFFECTIVE_STATUS + " IN $statuses",
                Map.of("listener", listener.value(), "statuses", names(statuses)), criteria);
    }

    @Override
    public List<TargetEventPublication> findCompletedPublications(PageCriteria criteria) {
        return page(completedLabel, "p.completionDate IS NOT NULL", Map.of(), criteria);
    }

    @Override
    public int markStalePublicationsFailed(Status status, Instant lastPublishedBefore) {
        return count(true, "MATCH (p:" + LABEL + ") WHERE p.status = $status AND p.completionDate IS NULL AND "
                + LAST_PUBLISHED_BEFORE + " SET p.status = 'FAILED' RETURN count(p)",
                Map.of("status", status.name(), "before", date(lastPublishedBefore)));
    }

    @Override
    public List<TargetEventPublication> findFailedPublications(FailedCriteria criteria) {
        StringBuilder cypher = new StringBuilder("MATCH (p:" + LABEL + ") WHERE p.completionDate IS NULL AND "
                + EFFECTIVE_STATUS + " = 'FAILED'");
        Map<String, Object> parameters = new HashMap<>();
        if (criteria.publishedBefore().isBefore(FAR_FUTURE)) {
            cypher.append(" AND p.publicationDate < $date");
            parameters.put("date", date(criteria.publishedBefore()));
        }
        cypher.append(RETURN_OLDEST_FIRST);
        if (criteria.maxItems() < Integer.MAX_VALUE) {
            cypher.append(" LIMIT $limit");
            parameters.put("limit", criteria.maxItems());
        }
        return read(cypher.toString(), parameters);
    }

    @Override
    public List<TargetEventPublication> findByStatus(Status status) {
        return read("MATCH (p:" + labelOf(status) + ") WHERE " + EFFECTIVE_STATUS + " = $status" + RETURN_OLDEST_FIRST,
                Map.of("status", status.name()));
    }

    @Override
    public int countByStatus(Status status) {
        return count(false, "MATCH (p:" + labelOf(status) + ") WHERE " + EFFECTIVE_STATUS + " = $status RETURN count(p)",
                Map.of("status", status.name()));
    }

    @Override
    public void deletePublications(List<UUID> identifiers) {
        deletePublicationsCounting(identifiers);
    }

    @Override
    public int deletePublicationsCounting(List<UUID> identifiers) {
        Map<String, Object> parameters = Map.of("identifiers", identifiers.stream().map(UUID::toString).toList());
        return write(tx -> tx.run("MATCH (p:" + LABEL + ") WHERE p.identifier IN $identifiers DELETE p RETURN count(p)",
                parameters).single().get(0).asInt()
                + tx.run("MATCH (p:" + ARCHIVE_LABEL + ") WHERE p.identifier IN $identifiers DELETE p RETURN count(p)",
                        parameters).single().get(0).asInt());
    }

    @Override
    public void deleteCompletedPublications() {
        deleteInBatches("p.completionDate IS NOT NULL", Map.of());
    }

    @Override
    public void deleteCompletedPublicationsBefore(Instant instant) {
        deleteCompletedPublicationsCompletedBefore(instant);
    }

    @Override
    public int deleteCompletedPublicationsCompletedBefore(Instant instant) {
        return deleteInBatches("p.completionDate < $date", Map.of("date", date(instant)));
    }

    /**
     * Deletes the completed nodes matching the condition, at most {@link #DELETE_BATCH_SIZE} per transaction.
     *
     * @return the number of deleted nodes
     */
    private int deleteInBatches(String condition, Map<String, Object> arguments) {
        String cypher = "MATCH (p:" + completedLabel + ") WHERE " + condition + " WITH p LIMIT $batch DELETE p "
                + "RETURN count(p)";
        Map<String, Object> parameters = new HashMap<>(arguments);
        parameters.put("batch", DELETE_BATCH_SIZE);
        int deleted = 0;
        int batch;
        do {
            batch = count(true, cypher, parameters);
            deleted += batch;
        } while (batch == DELETE_BATCH_SIZE);
        return deleted;
    }

    /**
     * Reads one page: the nodes matching the condition in the criteria's range, after its cursor, at most its size,
     * ordered by publication date, then identifier.
     */
    private List<TargetEventPublication> page(String label, String condition, Map<String, Object> arguments,
            PageCriteria criteria) {
        StringBuilder cypher = new StringBuilder("MATCH (p:" + label + ") WHERE " + condition);
        Map<String, Object> parameters = new HashMap<>(arguments);
        if (criteria.publishedBefore().isBefore(FAR_FUTURE)) {
            cypher.append(" AND p.publicationDate < $before");
            parameters.put("before", date(criteria.publishedBefore()));
        }
        criteria.after().ifPresent(cursor -> {
            cypher.append(" AND (p.publicationDate > $afterDate OR (p.publicationDate = $afterDate "
                    + "AND p.identifier > $afterId))");
            parameters.put("afterDate", date(cursor.publicationDate()));
            parameters.put("afterId", cursor.identifier().toString());
        });
        parameters.put("limit", criteria.maxItems());
        return read(cypher.append(RETURN_PAGE).toString(), parameters);
    }

    private static List<String> names(Set<Status> statuses) {
        return statuses.stream().map(Status::name).sorted().toList();
    }

    private String labelOf(Status status) {
        return status == Status.COMPLETED ? completedLabel : LABEL;
    }

    private String serialize(Object event) {
        return store.serializer().serialize(event).toString();
    }

    private <T> T write(TransactionCallback<T> work) {
        requireBlockingAllowed();
        try (Session session = store.driver().session(store.sessionConfig())) {
            return session.executeWrite(work);
        }
    }

    private int count(boolean write, String cypher, Map<String, Object> parameters) {
        TransactionCallback<Integer> work = tx -> tx.run(cypher, parameters).single().get(0).asInt();
        if (write) {
            return write(work);
        }
        requireBlockingAllowed();
        try (Session session = store.driver().session(store.sessionConfig())) {
            return session.executeRead(work);
        }
    }

    private List<TargetEventPublication> read(String cypher, Map<String, Object> parameters) {
        requireBlockingAllowed();
        try (Session session = store.driver().session(store.sessionConfig())) {
            return session.executeRead(tx -> {
                List<TargetEventPublication> result = new ArrayList<>();
                tx.run(cypher, parameters).list(record -> record.get("p").asNode())
                        .forEach(node -> map(node).ifPresent(result::add));
                return result;
            });
        }
    }

    Optional<TargetEventPublication> map(Node node) {
        UUID id = UUID.fromString(node.get("identifier").asString());
        String eventType = node.get("eventType").asString();
        Optional<Instant> completionDate = instant(node.get("completionDate"));
        Value status = node.get("status");
        return Optional.of(new StoredPublication(id, eventType, node.get("eventSerialized").asString(),
                store.serializer(), PublicationTargetIdentifier.of(node.get("listenerId").asString()),
                node.get("publicationDate").asZonedDateTime().toInstant(), completionDate,
                status.isNull() ? completionDate.map(date -> Status.COMPLETED).orElse(Status.FAILED)
                        : Status.valueOf(status.asString()),
                node.get("completionAttempts").asInt(0), instant(node.get("lastResubmissionDate"))));
    }

    private void requireBlockingAllowed() {
        if (!blockingAllowed.getAsBoolean()) {
            throw new IllegalStateException(BLOCKING_NOT_ALLOWED);
        }
    }

    private static Optional<Instant> instant(Value value) {
        return value.isNull() ? Optional.empty() : Optional.of(value.asZonedDateTime().toInstant());
    }

    private static Object date(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

}
