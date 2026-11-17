package io.quarkiverse.modulith.events.fixtures.app.order;

import io.quarkiverse.modulith.events.Externalized;

@Externalized
public record OrderPlaced(String id) {
}
