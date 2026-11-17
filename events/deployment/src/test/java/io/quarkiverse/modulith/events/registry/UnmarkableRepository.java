package io.quarkiverse.modulith.events.registry;

import java.util.UUID;

import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;

/**
 * A store that loses its connection whenever a delivery attempt is recorded.
 */
final class UnmarkableRepository extends InMemoryEventPublicationRepository {

    @Override
    public boolean markProcessing(UUID identifier) {
        throw new IllegalStateException("connection closed");
    }
}
