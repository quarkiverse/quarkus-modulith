package io.quarkiverse.modulith.it.eventsneo4j.repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.neo4j.driver.Driver;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.neo4j.runtime.EventHash;
import io.quarkiverse.modulith.events.tck.MissingStatusContract;
import io.quarkiverse.modulith.events.tck.TckEvent;
import io.quarkiverse.modulith.events.tck.TckEventSerializer;

/**
 * Nodes without status are written with plain Cypher, the way Spring Modulith 1.x wrote them.
 */
class Neo4jMissingStatusContractTest extends MissingStatusContract {

    private static final TckEventSerializer SERIALIZER = new TckEventSerializer();

    private final Driver driver = Neo4j.emptyServer();
    private final EventPublicationRepository repository = Neo4j.repository(driver,
            CompletionMode.UPDATE);

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }

    @Override
    protected UUID insertWithoutStatus(String eventId, Optional<Instant> completionDate) {
        UUID id = UUID.randomUUID();
        String serialized = SERIALIZER.serialize(new TckEvent(eventId)).toString();
        Map<String, Object> properties = new HashMap<>();
        properties.put("identifier", id.toString());
        properties.put("eventSerialized", serialized);
        properties.put("eventHash", EventHash.sha256(serialized));
        properties.put("eventType", TckEvent.class.getName());
        properties.put("listenerId", LISTENER.value());
        properties.put("publicationDate", T0.atOffset(ZoneOffset.UTC));
        properties.put("completionDate", completionDate.map(date -> date.atOffset(ZoneOffset.UTC)).orElse(null));
        driver.executableQuery("CREATE (p:Neo4jEventPublication) SET p = $properties")
                .withParameters(Map.of("properties", properties)).execute();
        return id;
    }
}
