package io.quarkiverse.modulith.events.fixtures.app.order;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("::{event.id}")
public record KeyOnly(String id) {
}
