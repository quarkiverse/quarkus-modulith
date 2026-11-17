package io.quarkiverse.modulith.events.core;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import io.quarkiverse.modulith.events.EventPublication.Status;

/**
 * The immutable publication behind the factory methods of {@link TargetEventPublication}.
 */
record Snapshot(UUID getIdentifier, Object getEvent, String getEventType, PublicationTargetIdentifier getTargetIdentifier,
        Instant getPublicationDate, Optional<Instant> getCompletionDate, Status getStatus, int getCompletionAttempts,
        Optional<Instant> getLastResubmissionDate) implements TargetEventPublication {
}
