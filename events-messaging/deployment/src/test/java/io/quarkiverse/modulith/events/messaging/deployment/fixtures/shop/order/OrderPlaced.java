package io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("orders::{event.id}")
public record OrderPlaced(String id, String tenant) {
}
