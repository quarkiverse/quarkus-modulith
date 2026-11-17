package io.quarkiverse.modulith.events.neo4j.runtime;

import java.util.List;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import org.neo4j.driver.Driver;
import org.neo4j.driver.SessionConfig;

import io.quarkiverse.modulith.events.config.EventsFixedConfig;
import io.quarkiverse.modulith.events.core.EventSerializer;
import io.quarkus.runtime.StartupEvent;

/**
 * Creates the {@link Neo4jStore}, and at startup, when asked to, the event hash index and a unique constraint on the
 * identifier of the publication and archive nodes.
 */
@Singleton
public class Neo4jStoreProducer {

    static final String CREATE_INDEX = "CREATE INDEX eventHashIndex IF NOT EXISTS "
            + "FOR (n:Neo4jEventPublication) ON (n.eventHash)";

    /**
     * Every lifecycle update looks a publication up by its identifier, so the constraint's index keeps it from
     * scanning every node of the label, and it forbids two archive nodes of one publication.
     */
    static final List<String> CREATE_CONSTRAINTS = List.of(
            "CREATE CONSTRAINT eventPublicationIdentifier IF NOT EXISTS "
                    + "FOR (n:Neo4jEventPublication) REQUIRE n.identifier IS UNIQUE",
            "CREATE CONSTRAINT eventPublicationArchiveIdentifier IF NOT EXISTS "
                    + "FOR (n:Neo4jEventPublicationArchive) REQUIRE n.identifier IS UNIQUE");

    @Produces
    @Singleton
    Neo4jStore store(Driver driver, EventSerializer serializer, Neo4jConfig config, EventsFixedConfig events) {
        SessionConfig sessionConfig = config.database().map(SessionConfig::forDatabase)
                .orElseGet(SessionConfig::defaultConfig);
        return new Neo4jStore(driver, sessionConfig, serializer, events.completionMode());
    }

    /**
     * Runs before the registry's own startup work, like Spring Modulith's {@code Neo4jIndexInitializer}, which only
     * creates the event hash index. Each statement runs in its own transaction.
     */
    void createIndex(@Observes @Priority(Interceptor.Priority.LIBRARY_BEFORE) StartupEvent event, Neo4jStore store,
            Neo4jConfig config) {
        if (config.eventIndex().enabled()) {
            try (var session = store.driver().session(store.sessionConfig())) {
                session.executeWriteWithoutResult(tx -> tx.run(CREATE_INDEX));
                CREATE_CONSTRAINTS.forEach(constraint -> session.executeWriteWithoutResult(tx -> tx.run(constraint)));
            }
        }
    }
}
