package io.quarkiverse.modulith.events.jms.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.JMSRuntimeException;
import jakarta.jms.TextMessage;

import org.jboss.logging.Logger;

import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.core.EventSerializer;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;

/**
 * Sends externalized events to JMS, like Spring Modulith's {@code JmsEventExternalizerConfiguration}: the routing
 * target names the destination, a queue unless {@code quarkus.modulith.events.jms.destination-type} says topic, the
 * event serialized with the {@link EventSerializer} is the body of a {@link TextMessage}, and the headers become message
 * properties. JMS has no routing key, so the key of the target is not sent.
 * <p>
 * Each send uses its own {@link JMSContext} of the application's {@link ConnectionFactory} (pool it with
 * {@code quarkus-pooled-jms} to reuse connections). Sending is synchronous: the stage completes with the
 * {@code JMSMessageID} once {@code send} returned, which for persistent messages means the broker stored the message.
 * The send runs on the executor, so the caller is never blocked, and the stage fails once
 * {@code quarkus.modulith.events.jms.send-timeout} passed without {@code send} returning. JMS cannot cancel a send, so
 * the worker stays busy until the provider gives up: configure a send timeout of the provider too
 * ({@code jms.sendTimeout} for Qpid JMS, {@code callTimeout} for Artemis).
 */
public class JmsEventExternalizationTransport implements EventExternalizationTransport {

    private static final Logger LOG = Logger.getLogger(JmsEventExternalizationTransport.class);

    private final ConnectionFactory connections;
    private final EventSerializer serializer;
    private final JmsDestinationType destinationType;
    private final Duration sendTimeout;
    private final Executor executor;

    public JmsEventExternalizationTransport(ConnectionFactory connections, EventSerializer serializer,
            JmsExternalizationConfig config, Executor executor) {
        this(connections, serializer, config.destinationType(), config.sendTimeout(), executor);
    }

    JmsEventExternalizationTransport(ConnectionFactory connections, EventSerializer serializer,
            JmsDestinationType destinationType, Duration sendTimeout, Executor executor) {
        this.connections = connections;
        this.serializer = serializer;
        this.destinationType = destinationType;
        this.sendTimeout = sendTimeout;
        this.executor = executor;
    }

    @Override
    public CompletionStage<?> externalize(Object payload, RoutingTarget target, Map<String, Object> headers) {
        return CompletableFuture.supplyAsync(() -> send(payload, target, headers), executor)
                .orTimeout(sendTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .exceptionallyCompose(failure -> CompletableFuture.failedFuture(failure instanceof TimeoutException
                        ? new TimeoutException("The JMS provider did not return from sending to the " + destinationType
                                + " " + target.getTarget() + " within " + sendTimeout
                                + " (quarkus.modulith.events.jms.send-timeout)")
                        : failure));
    }

    /**
     * Sends the message and waits until the provider returned from {@code send}.
     *
     * @return the {@code JMSMessageID} of the sent message
     */
    String send(Object payload, RoutingTarget target, Map<String, Object> headers) {
        Map<String, Object> properties = JmsProperties.of(headers);
        String body = serializer.serialize(payload).toString();
        try (JMSContext context = connections.createContext()) {
            Destination destination = destinationType == JmsDestinationType.TOPIC
                    ? context.createTopic(target.getTarget())
                    : context.createQueue(target.getTarget());
            TextMessage message = context.createTextMessage(body);
            for (Map.Entry<String, Object> property : properties.entrySet()) {
                message.setObjectProperty(property.getKey(), property.getValue());
            }
            context.createProducer().send(destination, message);
            String id = message.getJMSMessageID();
            LOG.debugf("Sent an event of type %s to the JMS %s %s as message %s", payload.getClass().getName(),
                    destinationType, target.getTarget(), id);
            return id;
        } catch (JMSException e) {
            throw new JMSRuntimeException("Could not send an event of type " + payload.getClass().getName()
                    + " to the JMS " + destinationType + " " + target.getTarget(), e.getErrorCode(), e);
        }
    }
}
