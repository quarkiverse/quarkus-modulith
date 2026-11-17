package io.quarkiverse.modulith.events.messaging.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkiverse.modulith.events.RoutingTarget;

/**
 * What a connector needs to address one message.
 *
 * @param target the routing target, with expressions resolved
 * @param headers the headers of the externalization configuration
 * @param dynamicTarget whether the message goes through the default channel, so that the connector must address the
 *        target per message, rather than through the channel named like the target
 * @param contentType the content type of the payload, empty when the connector decides
 */
public record OutgoingRouting(RoutingTarget target, Map<String, Object> headers, boolean dynamicTarget,
        Optional<String> contentType) {
}
