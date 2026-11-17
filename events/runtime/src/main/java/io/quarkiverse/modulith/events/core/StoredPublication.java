package io.quarkiverse.modulith.events.core;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A publication a store read back, for stores that keep the event as text. The event is deserialized on first access,
 * so a row whose event type is gone fails only where its event is used, not every read of a page. Deserializing is not
 * synchronized: two threads reading the event at once may both deserialize it.
 */
public final class StoredPublication implements TargetEventPublication {

    private final UUID identifier;
    private final String eventType;
    private final String serializedEvent;
    private final EventSerializer serializer;
    private final PublicationTargetIdentifier target;
    private final Instant publicationDate;
    private final Optional<Instant> completionDate;
    private final Status status;
    private final int completionAttempts;
    private final Optional<Instant> lastResubmissionDate;
    private Object event;

    public StoredPublication(UUID identifier, String eventType, String serializedEvent, EventSerializer serializer,
            PublicationTargetIdentifier target, Instant publicationDate, Optional<Instant> completionDate, Status status,
            int completionAttempts, Optional<Instant> lastResubmissionDate) {
        this.identifier = identifier;
        this.eventType = eventType;
        this.serializedEvent = serializedEvent;
        this.serializer = serializer;
        this.target = target;
        this.publicationDate = publicationDate;
        this.completionDate = completionDate;
        this.status = status;
        this.completionAttempts = completionAttempts;
        this.lastResubmissionDate = lastResubmissionDate;
    }

    @Override
    public UUID getIdentifier() {
        return identifier;
    }

    /**
     * @throws IllegalStateException if the event type is not on the class path anymore
     */
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
