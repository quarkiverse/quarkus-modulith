package io.quarkiverse.modulith.it.eventsmongodb.repository;

import org.bson.UuidRepresentation;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoDbEventPublicationRepository;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoStore;
import io.quarkiverse.modulith.events.mongodb.runtime.NoMongoSessions;
import io.quarkiverse.modulith.events.tck.DeleteCompletionModeContract;

class MongoDeleteContractTest extends DeleteCompletionModeContract {

    private final EventPublicationRepository repository = new MongoDbEventPublicationRepository(new MongoStore(
            Mongo.emptyDatabase("delete-contract"), new TckEventSerializer(), new NoMongoSessions(),
            CompletionMode.DELETE, UuidRepresentation.STANDARD));

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }
}
