package io.quarkiverse.modulith.it.eventsneo4j.repository;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.SessionConfig;
import org.testcontainers.neo4j.Neo4jContainer;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.neo4j.runtime.Neo4jEventPublicationRepository;
import io.quarkiverse.modulith.events.neo4j.runtime.Neo4jStore;
import io.quarkiverse.modulith.events.tck.TckEventSerializer;

/**
 * One Neo4j server for the repository tests of the test JVM, on the image Quarkus Dev Services uses.
 */
public final class Neo4j {

    private static Driver driver;

    private Neo4j() {
    }

    /**
     * @return a driver for the server, with every node deleted
     */
    public static synchronized Driver emptyServer() {
        if (driver == null) {
            Neo4jContainer container = new Neo4jContainer("neo4j:5.26.33").withAdminPassword("modulith-tests");
            container.start();
            driver = GraphDatabase.driver(container.getBoltUrl(), AuthTokens.basic("neo4j", "modulith-tests"));
        }
        driver.executableQuery("MATCH (n) DETACH DELETE n").execute();
        return driver;
    }

    /**
     * @return a repository on the server's default database, outside a Quarkus application, so blocking is allowed
     */
    public static EventPublicationRepository repository(Driver driver, CompletionMode completion) {
        return new Neo4jEventPublicationRepository(new Neo4jStore(driver, SessionConfig.defaultConfig(),
                new TckEventSerializer(), completion), () -> true);
    }
}
