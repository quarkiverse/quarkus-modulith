package io.quarkiverse.modulith.events.deployment.memory;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;

/**
 * An application-provided store, which is enough for the registry to exist. It names the SPI itself because the
 * index only knows implementations among application classes.
 */
@Singleton
public class MemoryRepository extends InMemoryEventPublicationRepository implements EventPublicationRepository {
}
