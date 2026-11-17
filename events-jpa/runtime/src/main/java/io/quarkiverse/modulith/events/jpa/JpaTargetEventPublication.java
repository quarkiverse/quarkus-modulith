package io.quarkiverse.modulith.events.jpa;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.quarkiverse.modulith.events.core.EventSerializer;
import io.quarkiverse.modulith.events.core.EventTypes;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;

/**
 * A publication read from the database. The event is deserialized on first access only, so listing or filtering by
 * metadata does not pay for it.
 */
final class JpaTargetEventPublication implements TargetEventPublication {

    private final UUID identifier;
    private final String eventType;
    private final String serializedEvent;
    private final PublicationTargetIdentifier target;
    private final Instant publicationDate;
    private final Optional<Instant> completionDate;
    private final Status status;
    private final int completionAttempts;
    private final Optional<Instant> lastResubmissionDate;
    private final EventSerializer serializer;
    private Object event;

    JpaTargetEventPublication(AbstractJpaEventPublication row, EventSerializer serializer) {
        this.identifier = row.id;
        this.eventType = row.eventType;
        this.serializedEvent = row.serializedEvent;
        this.target = PublicationTargetIdentifier.of(row.listenerId);
        this.publicationDate = row.publicationDate;
        this.completionDate = Optional.ofNullable(row.completionDate);
        this.status = Objects.requireNonNullElse(row.status,
                completionDate.isPresent() ? Status.COMPLETED : Status.FAILED);
        this.completionAttempts = Objects.requireNonNullElse(row.completionAttempts, 0);
        this.lastResubmissionDate = Optional.ofNullable(row.lastResubmissionDate);
        this.serializer = serializer;
    }

    @Override
    public UUID getIdentifier() {
        return identifier;
    }

    @Override
    public Object getEvent() {
        if (Objects.isNull(event)) {
            event = serializer.deserialize(serializedEvent, EventTypes.load(identifier, eventType));
        }
        return event;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public PublicationTargetIdentifier getTargetIdentifier() {
        return target;
    }

    @Override
    public Instant getPublicationDate() {
        return publicationDate;
    }

    @Override
    public Optional<Instant> getCompletionDate() {
        return completionDate;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public int getCompletionAttempts() {
        return completionAttempts;
    }

    @Override
    public Optional<Instant> getLastResubmissionDate() {
        return lastResubmissionDate;
    }
}
