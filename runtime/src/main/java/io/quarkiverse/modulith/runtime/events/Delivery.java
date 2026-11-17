package io.quarkiverse.modulith.runtime.events;

import java.util.Optional;
import java.util.UUID;

/**
 * One event handed to one listener.
 *
 * @param listener the target
 * @param event the event
 * @param publicationId the stored publication, empty without an event publication registry
 */
record Delivery(Listener listener, Object event, Optional<UUID> publicationId) {
}
