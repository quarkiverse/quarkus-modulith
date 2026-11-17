package io.quarkiverse.modulith.observability.runtime;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;

/**
 * An in-memory repository whose counts fail while {@link #down} is set.
 */
final class FlakyRepository extends InMemoryEventPublicationRepository {

    boolean down;

    @Override
    public int countByStatus(Status status) {
        if (down) {
            throw new IllegalStateException("store down");
        }
        return super.countByStatus(status);
    }
}
