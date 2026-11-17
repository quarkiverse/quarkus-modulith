package io.quarkiverse.modulith.events.neo4j.runtime;

import org.neo4j.driver.Driver;
import org.neo4j.driver.SessionConfig;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventSerializer;

/**
 * Everything the Neo4j event publication repository works with.
 *
 * @param driver the Neo4j driver
 * @param sessionConfig the session settings, with the database of the publication nodes
 * @param serializer turns events into the stored text and back
 * @param completion what happens to completed publications
 */
public record Neo4jStore(Driver driver, SessionConfig sessionConfig, EventSerializer serializer,
        CompletionMode completion) {
}
