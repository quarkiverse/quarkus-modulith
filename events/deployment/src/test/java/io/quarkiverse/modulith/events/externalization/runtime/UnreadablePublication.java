package io.quarkiverse.modulith.events.externalization.runtime;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * A stored publication whose event type is gone, as a store reads it back.
 */
record UnreadablePublication(TargetEventPublication stored) implements TargetEventPublication {

    @Override
    public UUID getIdentifier() {
        return stored.getIdentifier();
    }

    @Override
    public Object getEvent() {
        throw new IllegalStateException("Cannot read event publication " + stored.getIdentifier()
                + " of type com.acme.Removed");
    }

    @Override
    public String getEventType() {
        return "com.acme.Removed";
    }

    @Override
    public PublicationTargetIdentifier getTargetIdentifier() {
        return stored.getTargetIdentifier();
    }

    @Override
    public Instant getPublicationDate() {
        return stored.getPublicationDate();
    }

    @Override
    public Optional<Instant> getCompletionDate() {
        return stored.getCompletionDate();
    }

    @Override
    public Status getStatus() {
        return stored.getStatus();
    }

    @Override
    public int getCompletionAttempts() {
        return stored.getCompletionAttempts();
    }

    @Override
    public Optional<Instant> getLastResubmissionDate() {
        return stored.getLastResubmissionDate();
    }
}
