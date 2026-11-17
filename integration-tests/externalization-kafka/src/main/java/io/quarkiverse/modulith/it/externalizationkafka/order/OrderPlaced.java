package io.quarkiverse.modulith.it.externalizationkafka.order;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("orders::{event.id}")
public record OrderPlaced(String id, String tenant) {
}
