package io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("shipments::#{#this.id()}")
public record OrderShipped(String id) {
}
