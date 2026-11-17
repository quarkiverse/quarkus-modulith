package io.quarkiverse.modulith.events.deployment.externalization.fixtures.custom;

import java.util.Map;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.EventExternalizationConfiguration;
import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order.OrderNoted;

public class CustomConfiguration {

    @Produces
    @Singleton
    EventExternalizationConfiguration configuration() {
        return EventExternalizationConfiguration.externalizing()
                .selectByType(OrderNoted.class)
                .mapping(OrderNoted.class, OrderNoted::id)
                .headers(String.class, id -> Map.of("length", id.length()))
                .routeAll(event -> RoutingTarget.forTarget("notes"))
                .routeKey(OrderNoted.class, OrderNoted::id)
                .serializeExternalization()
                .build();
    }
}
