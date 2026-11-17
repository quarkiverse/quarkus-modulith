package io.quarkiverse.modulith.it.externalizationrabbitmq.order;

import java.util.Map;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.EventExternalizationConfiguration;

public class OrderExternalization {

    @Produces
    @Singleton
    EventExternalizationConfiguration configuration() {
        return EventExternalizationConfiguration.defaults("io.quarkiverse.modulith.it.externalizationrabbitmq")
                .headers(OrderPlaced.class, event -> Map.of("tenant", event.tenant()))
                .build();
    }
}
