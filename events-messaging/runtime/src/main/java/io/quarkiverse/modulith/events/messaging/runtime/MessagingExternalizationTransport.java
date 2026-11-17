package io.quarkiverse.modulith.events.messaging.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;
import io.quarkiverse.modulith.events.messaging.RoutingMetadata;
import io.quarkiverse.modulith.events.messaging.impl.MessagingChannels;
import io.quarkiverse.modulith.events.spi.ListenerExecutor;
import io.quarkus.arc.All;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.OutgoingMessageMetadata;

/**
 * Sends externalized events through Quarkus Messaging, like Spring Modulith's Kafka, AMQP and Spring Messaging
 * externalizers. The event goes to the channel named like its target, or else to the default channel. The returned
 * stage completes once the connector acknowledged the message, with the result the connector recorded in
 * {@link OutgoingMessageMetadata} (Kafka's {@code RecordMetadata}, RabbitMQ's delivery tag), on a worker thread.
 * <p>
 * Each channel buffers the messages its connector has not requested yet, up to
 * {@code mp.messaging.emitter.default-buffer-size} (128 by default). While the buffer is full, for instance while the
 * broker is unavailable, a send fails right away and the publication stays incomplete; the channel takes messages
 * again once the connector requests more.
 * <p>
 * The build registers the line-specific subclass {@code DefaultMessagingTransport}, which yields to a transport bean
 * of the application.
 */
public class MessagingExternalizationTransport implements EventExternalizationTransport {

    @Inject
    MessagingChannels channels;

    @Inject
    PayloadConverter payloads;

    @Inject
    @All
    List<ConnectorMetadata> connectors;

    @Inject
    MessagingRuntimeConfig config;

    @Inject
    ListenerExecutor executor;

    @Override
    public CompletionStage<?> externalize(Object payload, RoutingTarget target, Map<String, Object> headers) {
        Optional<Emitter<Object>> own = channels.channel(target.getTarget());
        Optional<Emitter<Object>> emitter = own.or(channels::defaultChannel);
        if (emitter.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("No Quarkus Messaging channel for the"
                    + " routing target " + target.getTarget() + ": add it to quarkus.modulith.events.messaging.channels,"
                    + " or set quarkus.modulith.events.messaging.default-channel"));
        }
        CompletableFuture<Object> acknowledged = new CompletableFuture<>();
        Message<?> message = message(payload, target, headers, own.isEmpty(), acknowledged);
        try {
            emitter.get().send(message);
        } catch (IllegalStateException e) {
            return CompletableFuture.failedFuture(new IllegalStateException("Quarkus Messaging refused the message"
                    + " sent to " + target + ": " + e.getMessage() + ". A full emitter buffer"
                    + " (mp.messaging.emitter.default-buffer-size) means the connector stopped requesting messages,"
                    + " for instance while the broker is unavailable", e));
        }
        Duration timeout = config.ackTimeout();
        return Uni.createFrom().completionStage(acknowledged)
                .ifNoItem().after(timeout).failWith(() -> new TimeoutException("The broker did not acknowledge the"
                        + " message sent to " + target + " within " + timeout
                        + " (quarkus.modulith.events.messaging.ack-timeout)"))
                .emitOn(executor::execute)
                .subscribeAsCompletionStage();
    }

    /**
     * Builds the message: a {@link Message} payload is sent as it is, with the metadata it lacks added, otherwise the
     * converted payload is sent with the routing metadata and the metadata of every connector present.
     */
    private Message<?> message(Object payload, RoutingTarget target, Map<String, Object> headers,
            boolean dynamicTarget, CompletableFuture<Object> acknowledged) {
        boolean given = payload instanceof Message<?>;
        Message<?> base = given ? (Message<?>) payload : Message.of(payloads.convert(payload));
        OutgoingRouting routing = new OutgoingRouting(target, headers, dynamicTarget,
                given ? Optional.empty() : payloads.contentType());
        List<Object> candidates = new ArrayList<>();
        candidates.add(RoutingMetadata.of(target, headers));
        connectors.forEach(connector -> candidates.add(connector.create(routing)));
        Message<?> message = base;
        for (Object metadata : candidates) {
            if (base.getMetadata(metadata.getClass()).isEmpty()) {
                message = message.addMetadata(metadata);
            }
        }
        OutgoingMessageMetadata<Object> result = new OutgoingMessageMetadata<>();
        return message.addMetadata(result)
                .withAck(() -> {
                    acknowledged.complete(result.getResult());
                    return base.ack();
                })
                .withNack(failure -> {
                    acknowledged.completeExceptionally(failure);
                    return base.nack(failure);
                });
    }
}
