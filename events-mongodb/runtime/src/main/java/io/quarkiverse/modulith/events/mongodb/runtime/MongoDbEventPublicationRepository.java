package io.quarkiverse.modulith.events.mongodb.runtime;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.bson.BsonBinary;
import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.conversions.Bson;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.jboss.logging.Logger;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.FailedCriteria;
import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.StoredPublication;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * The MongoDB event publication repository, after Spring Modulith's {@code MongoDbEventPublicationRepository}: the
 * {@code event_publication} collection with the same fields, the {@code event_publication_archive} collection in
 * archive completion mode. The event is stored as an embedded document carrying its type in {@code _class}, like
 * Spring Data's mapping does. Writes and reads use the client session of the current transaction when there is one.
 * <p>
 * The store creates no indexes, like Spring Modulith's. Collections that grow (completion mode {@code update}) are best
 * served by {@code {completionDate: 1, publicationDate: 1, _id: 1}} for the pages of incomplete and completed
 * publications, and {@code {listenerId: 1, status: 1}} for the pages of one listener.
 * <p>
 * A document without {@code status}, written by Spring Modulith 1.x, is {@code COMPLETED} when it has a completion
 * date and {@code FAILED} otherwise, in reads, status filters, claims and the failed publications, like the JDBC and
 * Neo4j stores and Spring Modulith 2.1.2. The staleness monitor leaves such documents alone.
 */
@Singleton
public class MongoDbEventPublicationRepository implements EventPublicationRepository {

    static final String COLLECTION = "event_publication";
    static final String ARCHIVE_COLLECTION = "event_publication_archive";

    static final String ID = "_id";
    static final String CLASS = "_class";
    static final String EVENT = "event";
    static final String EVENT_TYPE = "eventType";
    static final String LISTENER_ID = "listenerId";
    static final String PUBLICATION_DATE = "publicationDate";
    static final String COMPLETION_DATE = "completionDate";
    static final String STATUS = "status";
    static final String COMPLETION_ATTEMPTS = "completionAttempts";
    static final String LAST_RESUBMISSION_DATE = "lastResubmissionDate";

    private static final Bson OLDEST_FIRST = Sorts.ascending(PUBLICATION_DATE);
    private static final Bson PAGE_ORDER = Sorts.ascending(PUBLICATION_DATE, ID);
    private static final List<String> STALEABLE_NAMES = STALEABLE.stream().map(Status::name).sorted().toList();
    private static final JsonWriterSettings JSON = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();
    private static final Logger LOG = Logger.getLogger(MongoDbEventPublicationRepository.class);

    private final MongoStore store;
    private final MongoCollection<BsonDocument> publications;
    private final MongoCollection<BsonDocument> archive;
    private final MongoCollection<BsonDocument> completed;

    @Inject
    public MongoDbEventPublicationRepository(MongoStore store) {
        this.store = store;
        this.publications = store.database().getCollection(COLLECTION, BsonDocument.class);
        this.archive = store.database().getCollection(ARCHIVE_COLLECTION, BsonDocument.class);
        this.completed = store.completion() == CompletionMode.ARCHIVE ? archive : publications;
    }

    @Override
    public TargetEventPublication create(TargetEventPublication publication) {
        BsonDocument document = new BsonDocument(ID, id(publication.getIdentifier()))
                .append(PUBLICATION_DATE, date(publication.getPublicationDate()))
                .append(LISTENER_ID, new BsonString(publication.getTargetIdentifier().value()))
                .append(EVENT, event(publication.getEvent(), publication.getEventType()))
                .append(EVENT_TYPE, new BsonString(publication.getEventType()))
                .append(COMPLETION_DATE,
                        publication.getCompletionDate().<BsonValue> map(MongoDbEventPublicationRepository::date)
                                .orElse(BsonNull.VALUE))
                .append(STATUS, new BsonString(publication.getStatus().name()))
                .append(COMPLETION_ATTEMPTS, new BsonInt32(publication.getCompletionAttempts()))
                .append(LAST_RESUBMISSION_DATE,
                        publication.getLastResubmissionDate().<BsonValue> map(MongoDbEventPublicationRepository::date)
                                .orElse(BsonNull.VALUE));
        write(session -> publications.insertOne(session, document), () -> publications.insertOne(document), null);
        return publication;
    }

    @Override
    public boolean markProcessing(UUID identifier) {
        return update(and(eq(ID, id(identifier)), eq(COMPLETION_DATE, null)),
                combine(set(STATUS, Status.PROCESSING.name()), inc(COMPLETION_ATTEMPTS, 1))) == 1;
    }

    @Override
    public void markCompleted(UUID identifier, Instant completionDate) {
        Bson byId = eq(ID, id(identifier));
        CompletionMode completion = store.completion();
        if (completion == CompletionMode.DELETE) {
            delete(publications, byId);
        } else if (completion == CompletionMode.ARCHIVE) {
            Optional.ofNullable(find(publications, and(byId, eq(COMPLETION_DATE, null))).first()).ifPresent(document -> {
                BsonDocument archived = document.clone();
                archived.remove(ID);
                archived.put(COMPLETION_DATE, date(completionDate));
                archived.put(STATUS, new BsonString(Status.COMPLETED.name()));
                UpdateOptions upsert = new UpdateOptions().upsert(true);
                BsonDocument insertOnly = new BsonDocument("$setOnInsert", archived);
                write(session -> archive.updateOne(session, byId, insertOnly, upsert),
                        () -> archive.updateOne(byId, insertOnly, upsert), null);
            });
            delete(publications, byId);
        } else {
            update(and(byId, eq(COMPLETION_DATE, null)),
                    combine(set(COMPLETION_DATE, date(completionDate)), set(STATUS, Status.COMPLETED.name())));
        }
    }

    @Override
    public void markFailed(UUID identifier) {
        update(and(eq(ID, id(identifier)), eq(COMPLETION_DATE, null)), set(STATUS, Status.FAILED.name()));
    }

    @Override
    public boolean markResubmitted(UUID identifier, Instant resubmissionDate) {
        return update(and(eq(ID, id(identifier)), eq(COMPLETION_DATE, null), ne(STATUS, Status.RESUBMITTED.name())),
                combine(set(STATUS, Status.RESUBMITTED.name()), set(LAST_RESUBMISSION_DATE, date(resubmissionDate)))) == 1;
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications() {
        return read(find(publications, eq(COMPLETION_DATE, null)));
    }

    @Override
    public List<TargetEventPublication> findIncompletePublicationsPublishedBefore(Instant instant) {
        return read(find(publications, and(eq(COMPLETION_DATE, null), lt(PUBLICATION_DATE, date(instant)))));
    }

    /**
     * Matches the stored event as a whole embedded document, so field order counts: a document written by Spring Data
     * with its fields, or {@code _class}, in another order than the serializer's is not found.
     */
    @Override
    public Optional<TargetEventPublication> findIncompletePublicationsByEventAndTargetIdentifier(Object event,
            PublicationTargetIdentifier target) {
        Class<?> type = event.getClass();
        return read(find(publications, and(eq(EVENT, event(event, type.getName())), eq(LISTENER_ID, target.value()),
                eq(COMPLETION_DATE, null))).limit(1)).stream().findFirst();
    }

    @Override
    public List<TargetEventPublication> findCompletedPublications() {
        return read(find(completed, ne(COMPLETION_DATE, null)));
    }

    /**
     * One conditional update, so two instances never both claim a publication. It runs outside the client session: the
     * registry claims in a transaction of its own, and in a MongoDB transaction a concurrent claim of the same document
     * fails with a write conflict instead of matching nothing.
     */
    @Override
    public boolean claim(UUID identifier, Set<Status> eligible, Instant staleBefore, Instant claimDate) {
        Bson filter = and(eq(ID, id(identifier)), eq(COMPLETION_DATE, null),
                or(statusIn(eligible), and(in(STATUS, STALEABLE_NAMES), lastPublishedBefore(staleBefore))));
        Bson claimed = combine(set(STATUS, Status.RESUBMITTED.name()), set(LAST_RESUBMISSION_DATE, date(claimDate)));
        return !store.sessions().discardsWrites() && publications.updateOne(filter, claimed).getMatchedCount() == 1;
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications(PageCriteria criteria) {
        return read(page(publications, eq(COMPLETION_DATE, null), criteria));
    }

    @Override
    public List<TargetEventPublication> findIncompletePublications(PublicationTargetIdentifier listener,
            Set<Status> statuses, PageCriteria criteria) {
        return read(page(publications, and(eq(COMPLETION_DATE, null), eq(LISTENER_ID, listener.value()),
                statusIn(statuses)), criteria));
    }

    @Override
    public List<TargetEventPublication> findCompletedPublications(PageCriteria criteria) {
        return read(page(completed, ne(COMPLETION_DATE, null), criteria));
    }

    @Override
    public int markStalePublicationsFailed(Status status, Instant lastPublishedBefore) {
        Bson filter = and(eq(STATUS, status.name()), eq(COMPLETION_DATE, null), lastPublishedBefore(lastPublishedBefore));
        Bson failed = set(STATUS, Status.FAILED.name());
        return Math.toIntExact(write(session -> publications.updateMany(session, filter, failed).getModifiedCount(),
                () -> publications.updateMany(filter, failed).getModifiedCount(), 0L));
    }

    /**
     * Runs the deliveries once the MongoDB transaction of the publications committed. That transaction commits after
     * the JTA transaction, so the deliveries first commit it if it is still open. When that commit fails the deliveries
     * are skipped, since the listeners would not find the publications: the commit is retried when the JTA
     * transaction completes, and publications it stores stay incomplete until they are resubmitted.
     */
    @Override
    public void afterCommit(Runnable action, Consumer<Runnable> afterTransactionCommit) {
        afterTransactionCommit.accept(store.sessions().current().<Runnable> map(session -> () -> {
            try {
                commitPending(session);
            } catch (MongoException e) {
                LOG.errorf(e, "Committing the MongoDB transaction of event publications failed, so their deliveries "
                        + "are skipped. Publications stored by the retried commit stay incomplete until they are "
                        + "resubmitted, see quarkus.modulith.events.republish-interval");
                return;
            }
            action.run();
        }).orElse(action));
    }

    static void commitPending(ClientSession session) {
        if (session.hasActiveTransaction()) {
            session.commitTransaction();
        }
    }

    @Override
    public List<TargetEventPublication> findFailedPublications(FailedCriteria criteria) {
        List<Bson> filters = new ArrayList<>();
        filters.add(or(eq(STATUS, Status.FAILED.name()), and(eq(STATUS, null), eq(COMPLETION_DATE, null))));
        if (criteria.publishedBefore().isBefore(FAR_FUTURE)) {
            filters.add(lt(PUBLICATION_DATE, date(criteria.publishedBefore())));
        }
        FindIterable<BsonDocument> found = find(publications, and(filters));
        return read(criteria.maxItems() < Integer.MAX_VALUE ? found.limit(criteria.maxItems()) : found);
    }

    @Override
    public List<TargetEventPublication> findByStatus(Status status) {
        return read(find(collectionOf(status), byStatus(status)));
    }

    @Override
    public int countByStatus(Status status) {
        MongoCollection<BsonDocument> collection = collectionOf(status);
        Bson filter = byStatus(status);
        return Math.toIntExact(session(session -> collection.countDocuments(session, filter),
                () -> collection.countDocuments(filter)));
    }

    @Override
    public void deletePublications(List<UUID> identifiers) {
        deletePublicationsCounting(identifiers);
    }

    @Override
    public int deletePublicationsCounting(List<UUID> identifiers) {
        Bson byIds = in(ID, identifiers.stream().map(this::id).toList());
        return delete(publications, byIds) + delete(archive, byIds);
    }

    @Override
    public void deleteCompletedPublications() {
        delete(completed, ne(COMPLETION_DATE, null));
    }

    @Override
    public void deleteCompletedPublicationsBefore(Instant instant) {
        deleteCompletedPublicationsCompletedBefore(instant);
    }

    @Override
    public int deleteCompletedPublicationsCompletedBefore(Instant instant) {
        return delete(completed, lt(COMPLETION_DATE, date(instant)));
    }

    /**
     * Older publications can have a completion date without the status, as in Spring Modulith.
     */
    private static Bson byStatus(Status status) {
        return status == Status.COMPLETED ? ne(COMPLETION_DATE, null)
                : and(statusIn(Set.of(status)), eq(COMPLETION_DATE, null));
    }

    private MongoCollection<BsonDocument> collectionOf(Status status) {
        return status == Status.COMPLETED ? completed : publications;
    }

    private long update(Bson filter, Bson update) {
        return write(session -> publications.updateOne(session, filter, update).getMatchedCount(),
                () -> publications.updateOne(filter, update).getMatchedCount(), 0L);
    }

    private int delete(MongoCollection<BsonDocument> collection, Bson filter) {
        return Math.toIntExact(write(session -> collection.deleteMany(session, filter).getDeletedCount(),
                () -> collection.deleteMany(filter).getDeletedCount(), 0L));
    }

    /**
     * Reads one page: the documents matching the filter in the criteria's range, after its cursor, at most its size,
     * ordered by publication date, then identifier.
     */
    private FindIterable<BsonDocument> page(MongoCollection<BsonDocument> collection, Bson filter,
            PageCriteria criteria) {
        List<Bson> filters = new ArrayList<>(List.of(filter));
        if (criteria.publishedBefore().isBefore(FAR_FUTURE)) {
            filters.add(lt(PUBLICATION_DATE, date(criteria.publishedBefore())));
        }
        criteria.after().ifPresent(cursor -> filters.add(or(gt(PUBLICATION_DATE, date(cursor.publicationDate())),
                and(eq(PUBLICATION_DATE, date(cursor.publicationDate())), gt(ID, id(cursor.identifier()))))));
        return session(session -> collection.find(session, and(filters)), () -> collection.find(and(filters)))
                .sort(PAGE_ORDER).limit(criteria.maxItems());
    }

    /**
     * @return a filter on the statuses, a document without status counting as failed
     */
    private static Bson statusIn(Set<Status> statuses) {
        Bson named = in(STATUS, statuses.stream().map(Status::name).toList());
        return statuses.contains(Status.FAILED) ? or(named, eq(STATUS, null)) : named;
    }

    /**
     * @return a filter on the last publication date: the last resubmission, or else the publication
     */
    private Bson lastPublishedBefore(Instant instant) {
        return or(lt(LAST_RESUBMISSION_DATE, date(instant)),
                and(eq(LAST_RESUBMISSION_DATE, null), lt(PUBLICATION_DATE, date(instant))));
    }

    private FindIterable<BsonDocument> find(MongoCollection<BsonDocument> collection, Bson filter) {
        return session(session -> collection.find(session, filter), () -> collection.find(filter)).sort(OLDEST_FIRST);
    }

    private <T> T session(Function<ClientSession, T> inSession, Supplier<T> withoutSession) {
        return store.sessions().current().map(inSession).orElseGet(withoutSession);
    }

    /**
     * @return the result of the write, or {@code discarded} when the transaction drops writes
     */
    private <T> T write(Function<ClientSession, T> inSession, Supplier<T> withoutSession, T discarded) {
        return store.sessions().discardsWrites() ? discarded : session(inSession, withoutSession);
    }

    private List<TargetEventPublication> read(FindIterable<BsonDocument> documents) {
        List<TargetEventPublication> result = new ArrayList<>();
        documents.forEach(document -> result.add(map(document)));
        return result;
    }

    /**
     * @throws IllegalArgumentException for an unknown status, which fails the whole read rather than hiding the
     *         document
     */
    TargetEventPublication map(BsonDocument document) {
        UUID id = document.getBinary(ID).asUuid(store.uuidRepresentation());
        BsonValue event = document.get(EVENT);
        String eventType = document.containsKey(EVENT_TYPE) ? document.getString(EVENT_TYPE).getValue()
                : event.asDocument().getString(CLASS).getValue();
        Optional<Instant> completionDate = instant(document.get(COMPLETION_DATE));
        Status status = completionDate.isPresent() ? Status.COMPLETED
                : Status.valueOf(document.getString(STATUS, new BsonString(Status.FAILED.name())).getValue());
        return new StoredPublication(id, eventType, serialized(event), store.serializer(),
                PublicationTargetIdentifier.of(document.getString(LISTENER_ID).getValue()),
                Instant.ofEpochMilli(document.getDateTime(PUBLICATION_DATE).getValue()), completionDate, status,
                document.getInt32(COMPLETION_ATTEMPTS, new BsonInt32(0)).getValue(),
                instant(document.get(LAST_RESUBMISSION_DATE)));
    }

    /**
     * @return the event as an embedded document with its type in {@code _class} when it serializes to a JSON object,
     *         as a string otherwise
     */
    BsonValue event(Object event, String eventType) {
        String serialized = store.serializer().serialize(event).toString();
        if (!serialized.startsWith("{")) {
            return new BsonString(serialized);
        }
        BsonDocument document = new BsonDocument(CLASS, new BsonString(eventType));
        document.putAll(BsonDocument.parse(serialized));
        return document;
    }

    /**
     * @return the serialized form of a stored event: the text itself, or the embedded document without its type as
     *         JSON
     */
    private static String serialized(BsonValue event) {
        if (event.isString()) {
            return event.asString().getValue();
        }
        BsonDocument document = event.asDocument().clone();
        document.remove(CLASS);
        return document.toJson(JSON);
    }

    private BsonBinary id(UUID identifier) {
        return new BsonBinary(identifier, store.uuidRepresentation());
    }

    private static BsonDateTime date(Instant instant) {
        return new BsonDateTime(millis(instant));
    }

    /**
     * @return the instant in milliseconds, saturated to the range of a BSON date, so that bounds such as
     *         {@code Instant.MAX} and {@code Instant.MIN} match every or no document
     */
    static long millis(Instant instant) {
        try {
            return instant.toEpochMilli();
        } catch (ArithmeticException e) {
            return instant.isBefore(Instant.EPOCH) ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
    }

    private static Optional<Instant> instant(BsonValue value) {
        return Optional.ofNullable(value).filter(BsonValue::isDateTime)
                .map(date -> Instant.ofEpochMilli(date.asDateTime().getValue()));
    }

}
