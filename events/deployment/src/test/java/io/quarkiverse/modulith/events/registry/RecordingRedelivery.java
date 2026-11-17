package io.quarkiverse.modulith.events.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.quarkiverse.modulith.events.spi.EventRedelivery;
import io.quarkiverse.modulith.events.tck.TckEvent;

/**
 * Knows a set of listeners; throws for events whose id starts with {@code boom}.
 */
final class RecordingRedelivery implements EventRedelivery {

    final List<String> redelivered = new ArrayList<>();
    private final Set<String> listeners;

    RecordingRedelivery(Set<String> listeners) {
        this.listeners = listeners;
    }

    @Override
    public boolean redeliver(UUID publicationId, String listenerId, Object event) {
        String id = ((TckEvent) event).id();
        if (id.startsWith("boom")) {
            throw new IllegalStateException("cannot schedule " + id);
        }
        if (!listeners.contains(listenerId)) {
            return false;
        }
        redelivered.add(id);
        return true;
    }
}
