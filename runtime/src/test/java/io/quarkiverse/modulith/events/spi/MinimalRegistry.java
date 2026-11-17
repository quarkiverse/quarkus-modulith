package io.quarkiverse.modulith.events.spi;

import java.util.List;
import java.util.UUID;

/**
 * A registry implementing only the abstract methods, as a store written before {@code markProcessing} existed.
 */
final class MinimalRegistry implements EventPublicationRegistry {

    @Override
    public List<UUID> store(Object event, List<String> listenerIds) {
        return List.of();
    }

    @Override
    public void markCompleted(UUID publicationId) {
        // not needed
    }

    @Override
    public void markFailed(UUID publicationId, Throwable failure) {
        // not needed
    }
}
