package io.quarkiverse.modulith.it.externalizationamqp.order;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("{event.region}-orders::{event.id}")
public record OrderRerouted(String region, String id) {
}
