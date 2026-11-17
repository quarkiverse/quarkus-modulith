package io.quarkiverse.modulith.it.eventsmongodb.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bson.BsonBinary;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.junit.jupiter.api.Test;

import com.mongodb.client.MongoDatabase;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.EventSerializer;
import io.quarkiverse.modulith.events.core.FailedCriteria;
import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoDbEventPublicationRepository;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoStore;
import io.quarkiverse.modulith.events.mongodb.runtime.NoMongoSessions;
import io.quarkiverse.modulith.events.tck.TckEvent;

class MongoRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
    private static final PublicationTargetIdentifier LISTENER = PublicationTargetIdentifier.of("billing.on-order");

    @Test
    void deleteCompletionRemovesTheDocument() {
        // arrange
        EventPublicationRepository repository = repository(Mongo.emptyDatabase("delete"), CompletionMode.DELETE,
                new TckEventSerializer());
        TargetEventPublication completed = repository.create(publication("completed", 0));
        repository.create(publication("incomplete", 1));

        // act
        repository.markCompleted(completed.getIdentifier(), T0.plusSeconds(5));

        // assert
        assertThat(events(repository.findIncompletePublications())).containsExactly("incomplete");
        assertThat(repository.findCompletedPublications()).isEmpty();
    }

    @Test
    void storesTheFieldsOfSpringModulith() {
        // arrange
        MongoDatabase database = Mongo.emptyDatabase("fields");
        EventPublicationRepository repository = repository(database, CompletionMode.UPDATE, new TckEventSerializer());
        UUID id = UUID.fromString("6f1c9a52-1b0e-4c8e-9f55-2b7d7c1f3a10");

        // act
        repository.create(TargetEventPublication.of(id, new TckEvent("e-1"), TckEvent.class.getName(), LISTENER, T0,
                Optional.of(T0.plusSeconds(9)), Status.COMPLETED, 3, Optional.of(T0.plusSeconds(7))));

        // assert
        BsonDocument stored = database.getCollection("event_publication", BsonDocument.class).find().first();
        assertThat(stored.toJson()).isEqualTo("{\"_id\": {\"$binary\": {\"base64\": \"bxyaUhsOTI6fVSt9fB86EA==\", "
                + "\"subType\": \"04\"}}, \"publicationDate\": {\"$date\": \"2026-01-01T10:00:00Z\"}, "
                + "\"listenerId\": \"billing.on-order\", \"event\": {\"_class\": \"" + TckEvent.class.getName()
                + "\", \"id\": \"e-1\"}, \"eventType\": \"" + TckEvent.class.getName() + "\", "
                + "\"completionDate\": {\"$date\": \"2026-01-01T10:00:09Z\"}, \"status\": \"COMPLETED\", "
                + "\"completionAttempts\": 3, \"lastResubmissionDate\": {\"$date\": \"2026-01-01T10:00:07Z\"}}");
        assertThat(single(repository.findCompletedPublications())).satisfies(p -> {
            assertThat(p.getIdentifier()).isEqualTo(id);
            assertThat(p.getCompletionDate()).hasValue(T0.plusSeconds(9));
            assertThat(p.getLastResubmissionDate()).hasValue(T0.plusSeconds(7));
            assertThat(p.getCompletionAttempts()).isEqualTo(3);
        });
    }

    @Test
    void readsPublicationsOfUnknownEventTypesUntilTheirEventIsUsed() {
        // arrange
        EventPublicationRepository repository = repository(Mongo.emptyDatabase("unknown"), CompletionMode.UPDATE,
                new TckEventSerializer());
        repository.create(TargetEventPublication.of(UUID.randomUUID(), new TckEvent("gone"), "com.acme.Removed",
                LISTENER, T0, Optional.empty(), Status.PUBLISHED, 0, Optional.empty()));
        repository.create(publication("known", 1));

        // act
        List<TargetEventPublication> incomplete = repository.findIncompletePublications();
        Throwable thrown = catchThrowable(() -> incomplete.get(0).getEvent());

        // assert
        assertThat(incomplete).extracting(TargetEventPublication::getEventType)
                .containsExactly("com.acme.Removed", TckEvent.class.getName());
        assertThat(incomplete.get(1).getEvent()).isEqualTo(new TckEvent("known"));
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Cannot read event publication ").hasMessageEndingWith(" of type com.acme.Removed");
    }

    @Test
    void pagesWithoutAnUpperBound() {
        // arrange
        EventPublicationRepository repository = repository(Mongo.emptyDatabase("paging"), CompletionMode.UPDATE,
                new TckEventSerializer());
        repository.create(publication("first", 0));
        repository.create(publication("second", 1));

        // act
        List<TargetEventPublication> page = repository.findIncompletePublications(PageCriteria.first(Instant.MAX, 10));

        // assert
        assertThat(events(page)).containsExactly("first", "second");
    }

    @Test
    void storesEventsThatAreNotJsonObjectsAsText() {
        // arrange
        MongoDatabase database = Mongo.emptyDatabase("text");
        EventPublicationRepository repository = repository(database, CompletionMode.UPDATE, new TextSerializer());
        repository.create(publication("e-1", 0));

        // act
        Optional<TargetEventPublication> found = repository
                .findIncompletePublicationsByEventAndTargetIdentifier(new TckEvent("e-1"), LISTENER);

        // assert
        assertThat(found).map(TargetEventPublication::getEvent).hasValue(new TckEvent("e-1"));
        assertThat(database.getCollection("event_publication").find().first().getString("event")).isEqualTo("tck:e-1");
    }

    @Test
    void readsDocumentsWrittenBySpringModulith() {
        // arrange
        MongoDatabase database = Mongo.emptyDatabase("spring");
        EventPublicationRepository repository = repository(database, CompletionMode.UPDATE, new TckEventSerializer());
        UUID open = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID done = UUID.fromString("00000000-0000-0000-0000-000000000002");
        database.getCollection("event_publication").insertMany(List.of(
                springDocument(open, "open", null),
                springDocument(done, "done", Date.from(T0.plusSeconds(4)))));

        // act
        List<TargetEventPublication> failed = repository.findFailedPublications(FailedCriteria.all());
        List<TargetEventPublication> completed = repository.findCompletedPublications();

        // assert
        assertThat(single(failed)).satisfies(p -> {
            assertThat(p.getIdentifier()).isEqualTo(open);
            assertThat(p.getEvent()).isEqualTo(new TckEvent("open"));
            assertThat(p.getEventType()).isEqualTo(TckEvent.class.getName());
            assertThat(p.getStatus()).isEqualTo(Status.FAILED);
            assertThat(p.getCompletionAttempts()).isZero();
        });
        assertThat(single(completed)).satisfies(p -> {
            assertThat(p.getIdentifier()).isEqualTo(done);
            assertThat(p.getStatus()).isEqualTo(Status.COMPLETED);
            assertThat(p.getCompletionDate()).hasValue(T0.plusSeconds(4));
        });
    }

    private static Document springDocument(UUID id, String event, Date completionDate) {
        return new Document("_id", new BsonBinary(id, UuidRepresentation.STANDARD))
                .append("publicationDate", Date.from(T0))
                .append("listenerId", LISTENER.value())
                .append("event", new Document("id", event).append("_class", TckEvent.class.getName()))
                .append("completionDate", completionDate);
    }

    private static EventPublicationRepository repository(MongoDatabase database, CompletionMode completion,
            EventSerializer serializer) {
        return new MongoDbEventPublicationRepository(new MongoStore(database, serializer, new NoMongoSessions(),
                completion, UuidRepresentation.STANDARD));
    }

    private static TargetEventPublication publication(String event, int secondsAfterT0) {
        return TargetEventPublication.of(new TckEvent(event), LISTENER, T0.plusSeconds(secondsAfterT0));
    }

    private static TargetEventPublication single(List<TargetEventPublication> publications) {
        assertThat(publications).hasSize(1);
        return publications.get(0);
    }

    private static List<String> events(List<TargetEventPublication> publications) {
        return publications.stream().map(p -> ((TckEvent) p.getEvent()).id()).toList();
    }
}
