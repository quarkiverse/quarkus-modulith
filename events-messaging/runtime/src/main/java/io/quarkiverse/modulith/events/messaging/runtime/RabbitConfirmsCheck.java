package io.quarkiverse.modulith.events.messaging.runtime;

import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.Config;

import io.quarkiverse.modulith.events.messaging.impl.MessagingChannels;
import io.quarkus.runtime.StartupEvent;

/**
 * Fails the startup when a channel of the outbox mode turns RabbitMQ publisher confirms off, like Spring Modulith
 * asserting {@code spring.rabbitmq.publisher-confirm-type=correlated}: without confirms the RabbitMQ connector
 * acknowledges a message once it is written to the socket, and the outbox would mark events externalized that the
 * broker never received. Registered only in the outbox mode with {@code quarkus-messaging-rabbitmq}.
 */
@Singleton
public class RabbitConfirmsCheck {

    static final String PUBLISH_CONFIRMS = "publish-confirms";

    @Inject
    Config config;

    @Inject
    MessagingChannels channels;

    void check(@Observes StartupEvent startup) {
        List<String> unconfirmed = unconfirmed(config, channels);
        if (!unconfirmed.isEmpty()) {
            throw new IllegalStateException("The outbox externalization mode needs RabbitMQ publisher confirms, but"
                    + " they are off for the channels " + unconfirmed + ": remove "
                    + ChannelProperties.outgoing(unconfirmed.get(0), PUBLISH_CONFIRMS)
                    + "=false, or set quarkus.modulith.events.externalization.mode=module-listener");
        }
    }

    /**
     * @return the channels that turn RabbitMQ publisher confirms off
     */
    static List<String> unconfirmed(Config config, MessagingChannels channels) {
        return channels.names().stream()
                .filter(channel -> !config.getOptionalValue(ChannelProperties.outgoing(channel, PUBLISH_CONFIRMS),
                        Boolean.class).orElse(true))
                .toList();
    }
}
