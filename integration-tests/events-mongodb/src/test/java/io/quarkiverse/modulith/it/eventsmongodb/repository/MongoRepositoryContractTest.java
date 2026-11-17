package io.quarkiverse.modulith.it.eventsmongodb.repository;

import java.util.stream.Stream;

import org.bson.UuidRepresentation;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoDbEventPublicationRepository;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoStore;
import io.quarkiverse.modulith.events.mongodb.runtime.NoMongoSessions;
import io.quarkiverse.modulith.events.tck.EventPublicationRepositoryContract;

@ParameterizedClass(name = "{0}")
@MethodSource("targets")
class MongoRepositoryContractTest extends EventPublicationRepositoryContract {

    private final EventPublicationRepository repository;

    MongoRepositoryContractTest(MongoTarget target) {
        this.repository = new MongoDbEventPublicationRepository(new MongoStore(Mongo.emptyDatabase("contract"),
                new TckEventSerializer(), new NoMongoSessions(), target.completion(), target.uuidRepresentation()));
    }

    static Stream<MongoTarget> targets() {
        return Stream.of(new MongoTarget(CompletionMode.UPDATE, UuidRepresentation.STANDARD),
                new MongoTarget(CompletionMode.ARCHIVE, UuidRepresentation.STANDARD),
                new MongoTarget(CompletionMode.UPDATE, UuidRepresentation.JAVA_LEGACY),
                new MongoTarget(CompletionMode.ARCHIVE, UuidRepresentation.JAVA_LEGACY));
    }

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }
}
