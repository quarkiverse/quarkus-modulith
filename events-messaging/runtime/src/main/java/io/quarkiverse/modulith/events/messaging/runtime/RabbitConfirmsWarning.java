package io.quarkiverse.modulith.events.messaging.runtime;

import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.quarkiverse.modulith.events.messaging.impl.MessagingChannels;
import io.quarkus.runtime.StartupEvent;

/**
 * Warns at startup when a channel of the module-listener mode turns RabbitMQ publisher confirms off: the publication
 * completes on the acknowledgement of the message, which without confirms comes once the message is written to the
 * socket. Registered only in the module-listener mode with {@code quarkus-messaging-rabbitmq}; the outbox mode fails
 * the startup instead, see {@link RabbitConfirmsCheck}.
 */
@Singleton
public class RabbitConfirmsWarning {

    private static final Logger LOG = Logger.getLogger(RabbitConfirmsWarning.class);

    @Inject
    Config config;

    @Inject
    MessagingChannels channels;

    void check(@Observes StartupEvent startup) {
        List<String> unconfirmed = RabbitConfirmsCheck.unconfirmed(config, channels);
        if (!unconfirmed.isEmpty()) {
            LOG.warnf("RabbitMQ publisher confirms are off for the channels %s, so event publications complete once a"
                    + " message is written to the socket, even if the broker never stores it: remove %s=false",
                    unconfirmed, ChannelProperties.outgoing(unconfirmed.get(0), RabbitConfirmsCheck.PUBLISH_CONFIRMS));
        }
    }
}
