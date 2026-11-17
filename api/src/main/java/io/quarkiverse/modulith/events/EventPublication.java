package io.quarkiverse.modulith.events;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * The delivery of one event to one {@link io.quarkiverse.modulith.ApplicationModuleListener}, as stored by the event
 * publication registry.
 */
public interface EventPublication extends Comparable<EventPublication> {

    UUID getIdentifier();

    /**
     * @return the event, deserialized from the publication table
     */
    Object getEvent();

    String getEventType();

    String getListenerId();

    Instant getPublicationDate();

    Optional<Instant> getCompletionDate();

    Status getStatus();

    int getCompletionAttempts();

    /**
     * @return when the publication was last submitted again, empty if never
     */
    Optional<Instant> getLastResubmissionDate();

    /**
     * @return the last time the event was handed to its listener: the last resubmission, or else the publication
     */
    default Instant getLastPublicationDate() {
        return getLastResubmissionDate().orElse(getPublicationDate());
    }

    /**
     * Spring Modulith wraps the event in an {@code ApplicationEvent} here. CDI has no such type, so this returns the
     * event itself.
     *
     * @return the event
     */
    default Object getApplicationEvent() {
        return getEvent();
    }

    default boolean isCompleted() {
        return getCompletionDate().isPresent();
    }

    /**
     * Orders publications by publication date.
     */
    @Override
    default int compareTo(EventPublication other) {
        return getPublicationDate().compareTo(other.getPublicationDate());
    }

    /**
     * The lifecycle of a publication. The values match Spring Modulith's so that both can share a table.
     */
    enum Status {
        PUBLISHED,
        PROCESSING,
        COMPLETED,
        FAILED,
        RESUBMITTED
    }
}
