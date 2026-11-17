package io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("{event.region}-orders::{event.id}")
public record OrderRerouted(String region, String id) {
}
