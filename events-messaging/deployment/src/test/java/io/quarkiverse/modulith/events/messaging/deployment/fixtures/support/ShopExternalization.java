package io.quarkiverse.modulith.events.messaging.deployment.fixtures.support;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.EventExternalizationConfiguration;
import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderAudited;
import io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order.OrderPlaced;

public class ShopExternalization {

    @Produces
    @Singleton
    EventExternalizationConfiguration configuration() {
        return EventExternalizationConfiguration.defaults(ShopApplication.BASE_PACKAGE)
                .headers(OrderPlaced.class, ShopExternalization::headers)
                .route(OrderAudited.class, event -> RoutingTarget.forTarget("audit"))
                .build();
    }

    /**
     * @return the tenant header, {@code null} for an event without tenant
     */
    private static Map<String, Object> headers(OrderPlaced event) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("tenant", event.tenant());
        return headers;
    }
}
