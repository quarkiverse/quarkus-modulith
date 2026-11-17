package io.quarkiverse.modulith.events.messaging.runtime;

import java.util.Optional;

/**
 * Turns the mapped event into the payload of the message. The build registers the JSON or the plain implementation,
 * depending on {@code quarkus.modulith.events.messaging.enable-json}.
 */
public interface PayloadConverter {

    /**
     * @return the payload to send
     */
    Object convert(Object payload);

    /**
     * @return the content type of converted payloads, empty when the connector decides
     */
    Optional<String> contentType();
}
