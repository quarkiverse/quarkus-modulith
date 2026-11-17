package io.quarkiverse.modulith.events.spi;

import java.util.UUID;

/**
 * Submits a stored event publication to its listener again. Implemented by the module event dispatcher and used by
 * event publication registries to resubmit incomplete publications.
 * <p>
 * This is a service provider interface for Quarkus Modulith extensions, not for applications.
 */
public interface EventRedelivery {

    /**
     * Submits the event to the listener with the given identifier, the same way as a fresh delivery: on a worker
     * thread for asynchronous listeners, in a new transaction, marking the publication completed or failed.
     *
     * @param publicationId the identifier of the stored publication
     * @param listenerId the identifier of the listener, as stored with the publication
     * @param event the deserialized event
     * @return {@code false} if no listener with that identifier exists anymore
     */
    boolean redeliver(UUID publicationId, String listenerId, Object event);
}
