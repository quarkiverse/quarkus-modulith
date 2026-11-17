package io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("orders::{event.customer.id}")
public record Ordered(Customer customer) {
}
