package io.quarkiverse.modulith.events.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.inject.Inject;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.query.SelectionQuery;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.config.EventsFixedConfig;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.EventSerializer;
import io.quarkiverse.modulith.events.core.FailedCriteria;
import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * The event publication repository on Hibernate ORM, like Spring Modulith's {@code JpaEventPublicationRepository}.
 * Every filter runs in the database and pages are read with a keyset on publication date and identifier. Methods run
 * in the caller's transaction. A publication date bound at or after {@link EventPublicationRepository#FAR_FUTURE} is
 * no bound, so that {@link Instant#MAX} never reaches a database timestamp.
 * <p>
 * The bean is {@code DefaultJpaEventPublicationRepository}, a default bean that an application's own
 * {@link EventPublicationRepository} replaces.
 */
public class JpaEventPublicationRepository implements EventPublicationRepository {

    static final int DELETE_BATCH_SIZE = 100;
    static final int READ_PAGE_SIZE = 100;
    private static final String INCOMPLETE = "JpaEventPublication";
    private static final String ARCHIVED = "ArchivedJpaEventPublication";
    private static final String ORDER = " order by p.publicationDate, p.id";
    private static final String AFTER = " and (p.publicationDate > :afterDate"
            + " or (p.publicationDate = :afterDate and p.id > :afterId))";

    @Inject
    JpaSessions sessions;

    @Inject
    EventSerializer serializer;

    @Inject
    EventsFixedConfig config;

    /**
     * Persists the row. In a session that does not flush by itself, the session of a read-only listener transaction,
     * the row is flushed right away, or its deliveries would find nothing after the commit.
     */
    @Override
    public TargetEventPublication create(TargetEventPublication publication) {
        JpaEventPublication row = new JpaEventPublication();
        row.id = publication.getIdentifier();
        row.listenerId = publication.getListenerId();
        row.eventType = publication.getEventType();
        row.serializedEvent = serializer.serialize(publication.getEvent()).toString();
        row.publicationDate = publication.getPublicationDate();
        row.completionDate = publication.getCompletionDate().orElse(null);
        row.status = publication.getStatus();
        row.completionAttempts = publication.getCompletionAttempts();
        row.lastResubmissionDate = publication.getLastResubmissionDate().orElse(null);
        session().persist(row);
        if (session().getHibernateFlushMode() == FlushMode.MANUAL) {
            session().flush();
        }
        return publication;
    }

    @Override
    public boolean markProcessing(UUID identifier) {
        return Mutations.execute(session().createMutationQuery("update JpaEventPublication p set p.status = :status,"
                + " p.completionAttempts = coalesce(p.completionAttempts, 0) + 1"
                + " where p.id = :id and p.completionDate is null")
                .setParameter("status", Status.PROCESSING)
                .setParameter("id", identifier)) == 1;
    }

    @Override
    public void markCompleted(UUID identifier, Instant completionDate) {
        switch (config.completionMode()) {
            case DELETE -> Mutations.execute(session().createMutationQuery("delete from JpaEventPublication p where p.id = :id")
                    .setParameter("id", identifier));
            case ARCHIVE -> archive(identifier, completionDate);
            default -> Mutations.execute(session().createMutationQuery("update JpaEventPublication p set p.status = :status,"
                    + " p.completionDate = :date where p.id = :id")
                    .setParameter("status", Status.COMPLETED)
                    .setParameter("date", completionDate)
                    .setParameter("id", identifier));
        }
    }

    /**
     * Moves the row with mutation queries, which run right away whatever the flush mode, so a read-only listener
     * transaction (manual flushing) still archives its completion. A later second completion of the same publication
     * finds no row to move. Two completions racing each other make the second insert fail on the archive's key, which
     * rolls back the second listener transaction with timing {@code in-listener}: the first one completed it.
     */
    private void archive(UUID identifier, Instant completionDate) {
        Mutations.execute(session().createMutationQuery("insert into ArchivedJpaEventPublication (id, listenerId, eventType,"
                + " serializedEvent, publicationDate, completionDate, status, completionAttempts, lastResubmissionDate)"
                + " select p.id, p.listenerId, p.eventType, p.serializedEvent, p.publicationDate, p.completionDate,"
                + " p.status, p.completionAttempts, p.lastResubmissionDate from JpaEventPublication p where p.id = :id")
                .setParameter("id", identifier));
        Mutations.execute(session().createMutationQuery("update ArchivedJpaEventPublication p set p.status = :status,"
                + " p.completionDate = :date where p.id = :id")
                .setParameter("status", Status.COMPLETED)
                .setParameter("date", completionDate)
                .setParameter("id", identifier));
        Mutations.execute(session().createMutationQuery("delete from JpaEventPublication p where p.id = :id")
                .setParameter("id", identifier));
    }

    @Override
    public void markFailed(UUID identifier) {
        Mutations.execute(session().createMutationQuery("update JpaEventPublication p set p.status = :status"
                + " where p.id = :id and p.completionDate is null")
                .setParameter("status", Status.FAILED)
                .setParameter("id", identifier));
    }

    @Override
    public boolean markResubmitted(UUID identifier, Instant resubmissionDate) {
        return Mutations.execute(session().createMutationQuery("update JpaEventPublication p set p.status = :status,"
                + " p.lastResubmissionDate = :date where p.id = :id and p.completionDate is null"
                + " and (p.status is null or p.status <> :status)")
                .setParameter("status", Status.RESUBMITTED)
                .setParameter("date", resubmissionDate)
                .setParameter("id", identifier)) == 1;
    }

    /**
     * One conditional update, so concurrent instances cannot both claim a publication.
     */
    @Override
    public boolean claim(UUID identifier, Set<Status> eligible, Instant staleBefore, Instant claimDate) {
        return Mutations.execute(session().createMutationQuery("update JpaEventPublication p set p.status = :resubmitted,"
                + " p.lastResubmissionDate = :date where p.id = :id and p.completionDate is null and (p.status in :eligible"
                + (eligible.contains(Status.FAILED) ? " or p.status is null" : "")
                + " or (p.status in :staleable and coalesce(p.lastResubmissionDate, p.publicationDate) < :staleBefore))")
                .setParameter("resubmitted", Status.RESUBMITTED)
                .setParameter("date", claimDate)
                .setParameter("id", identifier)
                .setParameter("eligible", eligible)
                .setParameter("staleable", STALEABLE)
                .setParameter("staleBefore", staleBefore)) == 1;
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications(PublicationTargetIdentifier listener,
            Set<Status> statuses, PageCriteria criteria) {
        return read(page(INCOMPLETE, "p.completionDate is null and p.listenerId = :listener and (p.status in :statuses"
                + (statuses.contains(Status.FAILED) ? " or p.status is null" : "") + ")", criteria)
                .setParameter("listener", listener.value())
                .setParameter("statuses", statuses));
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications() {
        return read(select(INCOMPLETE, "p.completionDate is null" + ORDER));
    }

    @Override
    public List<TargetEventPublication> findIncompletePublicationsPublishedBefore(Instant instant) {
        return read(bounded(select(INCOMPLETE, "p.completionDate is null" + before(instant) + ORDER), instant));
    }

    /**
     * Compares the serialized event in Java, page by page over the listener's incomplete publications: comparing a
     * long text column with {@code =} fails on Oracle ({@code CLOB}) and has no index anywhere.
     */
    @Override
    public Optional<TargetEventPublication> findIncompletePublicationsByEventAndTargetIdentifier(Object event,
            PublicationTargetIdentifier target) {
        String serialized = serializer.serialize(event).toString();
        PageCriteria criteria = PageCriteria.first(FAR_FUTURE, READ_PAGE_SIZE);
        List<AbstractJpaEventPublication> page;
        do {
            page = page(INCOMPLETE, "p.completionDate is null and p.listenerId = :listener", criteria)
                    .setParameter("listener", target.value()).getResultList();
            for (AbstractJpaEventPublication row : page) {
                if (row.serializedEvent.equals(serialized)) {
                    return Optional.of(new JpaTargetEventPublication(row, serializer));
                }
            }
            criteria = page.isEmpty() ? criteria
                    : criteria.next(new JpaTargetEventPublication(page.get(page.size() - 1), serializer));
        } while (page.size() == criteria.maxItems());
        return Optional.empty();
    }

    @Override
    public List<TargetEventPublication> findCompletedPublications() {
        return read(select(completedEntity(), "p.completionDate is not null" + ORDER));
    }

    @Override
    public List<TargetEventPublication> findFailedPublications(FailedCriteria criteria) {
        return read(bounded(select(INCOMPLETE, "(p.status = :failed or (p.status is null and p.completionDate is null))"
                + before(criteria.publishedBefore()) + ORDER), criteria.publishedBefore())
                .setParameter("failed", Status.FAILED)
                .setMaxResults(criteria.maxItems()));
    }

    @Override
    public List<TargetEventPublication> findByStatus(Status status) {
        return read(select(entityFor(status), withStatus(status) + ORDER).setParameter("status", status));
    }

    @Override
    public int countByStatus(Status status) {
        return session().createSelectionQuery("select count(p) from " + entityFor(status)
                + " p where " + withStatus(status), Long.class)
                .setParameter("status", status)
                .getSingleResult()
                .intValue();
    }

    @Override
    public void deletePublications(List<UUID> identifiers) {
        deletePublicationsCounting(identifiers);
    }

    @Override
    public int deletePublicationsCounting(List<UUID> identifiers) {
        int deleted = 0;
        for (int from = 0; from < identifiers.size(); from += DELETE_BATCH_SIZE) {
            List<UUID> batch = identifiers.subList(from, Math.min(from + DELETE_BATCH_SIZE, identifiers.size()));
            for (String entity : entities()) {
                deleted += Mutations.execute(session().createMutationQuery("delete from " + entity + " p where p.id in :ids")
                        .setParameter("ids", batch));
            }
        }
        return deleted;
    }

    private List<String> entities() {
        return config.completionMode() == CompletionMode.ARCHIVE ? List.of(INCOMPLETE, ARCHIVED) : List.of(INCOMPLETE);
    }

    @Override
    public void deleteCompletedPublications() {
        Mutations.execute(
                session().createMutationQuery("delete from " + completedEntity() + " p where p.completionDate is not null"));
    }

    @Override
    public void deleteCompletedPublicationsBefore(Instant instant) {
        deleteCompletedPublicationsCompletedBefore(instant);
    }

    @Override
    public int deleteCompletedPublicationsCompletedBefore(Instant instant) {
        return Mutations.execute(
                session().createMutationQuery("delete from " + completedEntity() + " p where p.completionDate < :before")
                        .setParameter("before", instant));
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications(PageCriteria criteria) {
        return read(page(INCOMPLETE, "p.completionDate is null", criteria));
    }

    @Override
    public List<TargetEventPublication> findCompletedPublications(PageCriteria criteria) {
        return read(page(completedEntity(), "p.completionDate is not null", criteria));
    }

    @Override
    public int markStalePublicationsFailed(Status status, Instant lastPublishedBefore) {
        return Mutations.execute(session().createMutationQuery("update JpaEventPublication p set p.status = :failed"
                + " where p.status = :status and p.completionDate is null"
                + " and coalesce(p.lastResubmissionDate, p.publicationDate) < :before")
                .setParameter("failed", Status.FAILED)
                .setParameter("status", status)
                .setParameter("before", lastPublishedBefore));
    }

    private SelectionQuery<AbstractJpaEventPublication> page(String entity, String condition, PageCriteria criteria) {
        SelectionQuery<AbstractJpaEventPublication> query = bounded(select(entity, condition
                + before(criteria.publishedBefore()) + (criteria.after().isPresent() ? AFTER : "") + ORDER),
                criteria.publishedBefore())
                .setMaxResults(criteria.maxItems());
        criteria.after().ifPresent(cursor -> query.setParameter("afterDate", cursor.publicationDate())
                .setParameter("afterId", cursor.identifier()));
        return query;
    }

    private static boolean isBounded(Instant publishedBefore) {
        return publishedBefore.isBefore(FAR_FUTURE);
    }

    private static String before(Instant publishedBefore) {
        return isBounded(publishedBefore) ? " and p.publicationDate < :before" : "";
    }

    private static SelectionQuery<AbstractJpaEventPublication> bounded(SelectionQuery<AbstractJpaEventPublication> query,
            Instant publishedBefore) {
        return isBounded(publishedBefore) ? query.setParameter("before", publishedBefore) : query;
    }

    private SelectionQuery<AbstractJpaEventPublication> select(String entity, String condition) {
        return session().createSelectionQuery("from " + entity + " p where " + condition,
                AbstractJpaEventPublication.class);
    }

    private List<TargetEventPublication> read(SelectionQuery<AbstractJpaEventPublication> query) {
        return query.getResultList().stream()
                .<TargetEventPublication> map(row -> new JpaTargetEventPublication(row, serializer)).toList();
    }

    private String completedEntity() {
        return config.completionMode() == CompletionMode.ARCHIVE ? ARCHIVED : INCOMPLETE;
    }

    /**
     * Rows without status, written by Spring Modulith 1.x, count as {@code FAILED} while incomplete and as
     * {@code COMPLETED} once completed, the way Spring Modulith 2.1.2 resubmits them.
     */
    private static String withStatus(Status status) {
        return switch (status) {
            case FAILED -> "(p.status = :status or (p.status is null and p.completionDate is null))";
            case COMPLETED -> "(p.status = :status or (p.status is null and p.completionDate is not null))";
            default -> "p.status = :status";
        };
    }

    private String entityFor(Status status) {
        return status == Status.COMPLETED ? completedEntity() : INCOMPLETE;
    }

    private Session session() {
        return sessions.session();
    }
}
