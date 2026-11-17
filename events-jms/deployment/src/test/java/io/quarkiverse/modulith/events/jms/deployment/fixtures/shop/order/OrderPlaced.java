package io.quarkiverse.modulith.events.jms.deployment.fixtures.shop.order;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("orders")
public record OrderPlaced(String id) {
}
