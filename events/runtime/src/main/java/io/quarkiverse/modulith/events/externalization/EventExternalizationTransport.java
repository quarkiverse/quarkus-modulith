package io.quarkiverse.modulith.events.externalization;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import io.quarkiverse.modulith.events.RoutingTarget;

/**
 * Sends externalized events to a broker. Each transport extension (Quarkus Messaging, JMS) provides one CDI bean of
 * this type, like Spring Modulith's broker-specific externalizers. An application may declare its own bean instead.
 */
@FunctionalInterface
public interface EventExternalizationTransport {

    /**
     * Sends the payload. The returned stage completes when the broker acknowledged the message, and the publication is
     * marked completed only then. A failed stage leaves the publication incomplete. Must not block the calling thread
     * for longer than it takes to hand the message over.
     *
     * @param payload the mapped event
     * @param target where to send it, with expressions already resolved
     * @param headers headers to send with it, possibly empty
     * @return the broker's result, for example Kafka's {@code RecordMetadata}, or {@code null} in the stage when the
     *         broker returns nothing
     */
    CompletionStage<?> externalize(Object payload, RoutingTarget target, Map<String, Object> headers);
}
