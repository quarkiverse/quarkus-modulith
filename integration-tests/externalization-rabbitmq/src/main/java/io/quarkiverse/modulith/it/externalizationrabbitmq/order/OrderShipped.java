package io.quarkiverse.modulith.it.externalizationrabbitmq.order;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("shipments::#{#this.id()}")
public record OrderShipped(String id) {
}
