package io.quarkiverse.modulith.events.fixtures.app.order;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("shipments::#{#this.orderId()}")
public record OrderShipped(String orderId) {
}
