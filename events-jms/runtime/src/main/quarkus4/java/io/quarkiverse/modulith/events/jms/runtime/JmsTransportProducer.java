package io.quarkiverse.modulith.events.jms.runtime;


import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Reserve;
import jakarta.inject.Singleton;
import jakarta.jms.ConnectionFactory;

import io.quarkiverse.modulith.events.core.EventSerializer;
import io.quarkiverse.modulith.events.externalization.EventExternalizationTransport;
import io.quarkiverse.modulith.events.spi.ListenerExecutor;

/**
 * Produces the JMS transport as a reserve bean, so that an {@link EventExternalizationTransport} bean of the
 * application replaces it. The connection factory is looked up when the transport is created, so that an application
 * with a transport of its own needs none. Used with Quarkus 4.
 */
@Singleton
public class JmsTransportProducer {

    @Produces
    @Singleton
    @Reserve
    @Priority(0)
    EventExternalizationTransport jmsTransport(Instance<ConnectionFactory> connections, EventSerializer serializer,
            JmsExternalizationConfig config, ListenerExecutor executor) {
        return new JmsEventExternalizationTransport(connections.get(), serializer, config, executor::execute);
    }
}
