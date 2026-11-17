package io.quarkiverse.modulith.it.eventsmongodb.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.bson.BsonBinary;
import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.UuidRepresentation;
import org.junit.jupiter.api.BeforeEach;

import com.mongodb.client.MongoDatabase;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoDbEventPublicationRepository;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoStore;
import io.quarkiverse.modulith.events.mongodb.runtime.NoMongoSessions;
import io.quarkiverse.modulith.events.tck.MissingStatusContract;
import io.quarkiverse.modulith.events.tck.TckEvent;

/**
 * Documents without status are inserted directly, with the fields Spring Modulith 1.x wrote: no {@code status},
 * {@code completionAttempts}, {@code lastResubmissionDate} or {@code eventType}, the event type only in {@code _class}.
 */
class MongoMissingStatusContractTest extends MissingStatusContract {

    private MongoDatabase database;
    private EventPublicationRepository repository;

    @BeforeEach
    void emptyDatabase() {
        database = Mongo.emptyDatabase("missing-status");
        repository = new MongoDbEventPublicationRepository(new MongoStore(database, new TckEventSerializer(),
                new NoMongoSessions(), CompletionMode.UPDATE, UuidRepresentation.STANDARD));
    }

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }

    @Override
    protected UUID insertWithoutStatus(String eventId, Optional<Instant> completionDate) {
        UUID id = UUID.randomUUID();
        BsonDocument document = new BsonDocument("_id", new BsonBinary(id, UuidRepresentation.STANDARD))
                .append("publicationDate", new BsonDateTime(T0.toEpochMilli()))
                .append("listenerId", new BsonString(LISTENER.value()))
                .append("event", new BsonDocument("_class", new BsonString(TckEvent.class.getName()))
                        .append("id", new BsonString(eventId)));
        completionDate.ifPresent(date -> document.append("completionDate", new BsonDateTime(date.toEpochMilli())));
        database.getCollection("event_publication", BsonDocument.class).insertOne(document);
        return id;
    }
}
