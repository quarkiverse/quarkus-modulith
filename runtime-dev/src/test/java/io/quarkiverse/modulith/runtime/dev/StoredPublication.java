package io.quarkiverse.modulith.runtime.dev;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import io.quarkiverse.modulith.events.EventPublication;

/**
 * An incomplete publication as a store would return it.
 */
record StoredPublication(UUID getIdentifier, Instant getPublicationDate, Optional<Instant> getLastResubmissionDate)
        implements
            EventPublication {

    @Override
    public Object getEvent() {
        return "payload";
    }

    @Override
    public String getEventType() {
        return "com.acme.OrderCompleted";
    }

    @Override
    public String getListenerId() {
        return "com.acme.Inventory.on(com.acme.OrderCompleted)";
    }

    @Override
    public Optional<Instant> getCompletionDate() {
        return Optional.empty();
    }

    @Override
    public Status getStatus() {
        return Status.FAILED;
    }

    @Override
    public int getCompletionAttempts() {
        return 2;
    }
}
