package io.quarkiverse.modulith.events.jpa.deployment.externalized;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("shipments::{event.id}")
public record ShipmentDispatched(String id) {
}
