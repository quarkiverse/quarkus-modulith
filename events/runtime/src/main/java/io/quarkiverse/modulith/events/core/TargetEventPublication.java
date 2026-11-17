package io.quarkiverse.modulith.events.core;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import io.quarkiverse.modulith.events.EventPublication;

/**
 * An event publication bound to one listener, as stored by an {@link EventPublicationRepository}.
 */
public interface TargetEventPublication extends EventPublication {

    /**
     * @return the listener the publication is meant for
     */
    PublicationTargetIdentifier getTargetIdentifier();

    @Override
    default String getListenerId() {
        return getTargetIdentifier().value();
    }

    /**
     * @return a new publication with a random identifier, status {@code PUBLISHED} and no completion attempt yet
     */
    static TargetEventPublication of(Object event, PublicationTargetIdentifier target, Instant publicationDate) {
        return new Snapshot(UUID.randomUUID(), event, event.getClass().getName(), target, publicationDate,
                Optional.empty(), Status.PUBLISHED, 0, Optional.empty());
    }

    /**
     * @return an immutable publication with every field given, for repositories that read rows back
     */
    static TargetEventPublication of(UUID identifier, Object event, String eventType, PublicationTargetIdentifier target,
            Instant publicationDate, Optional<Instant> completionDate, Status status, int completionAttempts,
            Optional<Instant> lastResubmissionDate) {
        return new Snapshot(identifier, event, eventType, target, publicationDate, completionDate, status,
                completionAttempts, lastResubmissionDate);
    }
}
