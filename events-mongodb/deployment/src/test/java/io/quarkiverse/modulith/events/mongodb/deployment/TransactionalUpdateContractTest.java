package io.quarkiverse.modulith.events.mongodb.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.tck.EventPublicationRepositoryContract;
import io.quarkiverse.modulith.events.tck.Publications;
import io.quarkiverse.modulith.events.tck.TckEvent;
import io.quarkus.test.QuarkusExtensionTest;

public class TransactionalUpdateContractTest extends EventPublicationRepositoryContract {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TransactionalRepository.class, TckEvent.class, Publications.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.mongodb.devservices.image-name", "mongo:7.0.48")
            .overrideConfigKey("quarkus.mongodb.database", "contract")
            .overrideConfigKey("quarkus.modulith.events.completion-mode", "update");

    @Inject
    EventPublicationRepository repository;

    @Inject
    MongoClient client;

    @BeforeEach
    void dropCollections() {
        client.getDatabase("contract").drop();
    }

    @Override
    protected EventPublicationRepository repository() {
        return new TransactionalRepository(repository);
    }
}
