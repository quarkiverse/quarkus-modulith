package io.quarkiverse.modulith.events.neo4j.deployment;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.neo4j.driver.Driver;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.tck.EventPublicationRepositoryContract;
import io.quarkiverse.modulith.events.tck.Publications;
import io.quarkiverse.modulith.events.tck.TckEvent;
import io.quarkus.test.QuarkusExtensionTest;

public class Neo4jContractTest extends EventPublicationRepositoryContract {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TckEvent.class, Publications.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.neo4j.devservices.image-name", "neo4j:5.26.33")
            .overrideConfigKey("quarkus.modulith.events.neo4j.database", "neo4j")
            .overrideConfigKey("quarkus.modulith.events.completion-mode", "update");

    @Inject
    EventPublicationRepository repository;

    @Inject
    Driver driver;

    @BeforeEach
    void deleteNodes() {
        driver.executableQuery("MATCH (n) DETACH DELETE n").execute();
    }

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }
}
