package io.quarkiverse.modulith.it.eventsmongodb.repository;

import org.bson.UuidRepresentation;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoDbEventPublicationRepository;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoStore;
import io.quarkiverse.modulith.events.mongodb.runtime.NoMongoSessions;
import io.quarkiverse.modulith.events.tck.ArchiveCompletionModeContract;

class MongoArchiveContractTest extends ArchiveCompletionModeContract {

    private final EventPublicationRepository repository = new MongoDbEventPublicationRepository(new MongoStore(
            Mongo.emptyDatabase("archive-contract"), new TckEventSerializer(), new NoMongoSessions(),
            CompletionMode.ARCHIVE, UuidRepresentation.STANDARD));

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }
}
