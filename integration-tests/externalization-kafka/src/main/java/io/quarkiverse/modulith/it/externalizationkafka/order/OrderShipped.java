package io.quarkiverse.modulith.it.externalizationkafka.order;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("shipments::#{#this.id()}")
public record OrderShipped(String id) {
}
