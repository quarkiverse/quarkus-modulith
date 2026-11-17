package io.quarkiverse.modulith.events.mongodb.deployment;

import jakarta.inject.Inject;

import org.bson.UuidRepresentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.EventSerializer;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoDbEventPublicationRepository;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoSessions;
import io.quarkiverse.modulith.events.mongodb.runtime.MongoStore;
import io.quarkiverse.modulith.events.tck.EventPublicationRepositoryContract;
import io.quarkiverse.modulith.events.tck.Publications;
import io.quarkiverse.modulith.events.tck.TckEvent;
import io.quarkus.test.QuarkusExtensionTest;

public class TransactionalArchiveContractTest extends EventPublicationRepositoryContract {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TransactionalRepository.class, TckEvent.class, Publications.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.mongodb.devservices.image-name", "mongo:7.0.48");

    @Inject
    MongoClient client;

    @Inject
    MongoSessions sessions;

    @Inject
    EventSerializer serializer;

    @BeforeEach
    void dropCollections() {
        client.getDatabase("archive").drop();
    }

    @Override
    protected EventPublicationRepository repository() {
        return new TransactionalRepository(new MongoDbEventPublicationRepository(new MongoStore(
                client.getDatabase("archive"), serializer, sessions, CompletionMode.ARCHIVE,
                UuidRepresentation.JAVA_LEGACY)));
    }
}
