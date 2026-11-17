package io.quarkiverse.modulith.it.externalizationjms.order;

import java.util.Map;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.EventExternalizationConfiguration;

public class OrderExternalization {

    @Produces
    @Singleton
    EventExternalizationConfiguration configuration() {
        return EventExternalizationConfiguration.externalizing()
                .annotatedAsExternalized()
                .headers(OrderPlaced.class, event -> Map.of("orderId", event.id(), "lineCount", event.lines().size()))
                .build();
    }
}
