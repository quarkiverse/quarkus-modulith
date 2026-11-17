package io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("orders::{event.id}")
public record OrderPlaced(String id, int quantity) {
}
