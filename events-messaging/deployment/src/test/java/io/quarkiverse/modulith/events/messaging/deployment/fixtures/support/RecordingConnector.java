package io.quarkiverse.modulith.events.messaging.deployment.fixtures.support;

import java.util.concurrent.Flow;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;

import io.smallrye.reactive.messaging.connector.OutboundConnector;

@ApplicationScoped
@Connector(RecordingConnector.NAME)
public class RecordingConnector implements OutboundConnector {

    public static final String NAME = "modulith-recording";

    @Inject
    RecordingBroker broker;

    @Override
    public Flow.Subscriber<? extends Message<?>> getSubscriber(Config config) {
        return new RecordingSubscriber(config.getValue("channel-name", String.class), broker);
    }
}
