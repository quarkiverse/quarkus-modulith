package io.quarkiverse.modulith.events.deployment.externalization.fixtures.unnamed;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("loose::{inject:loose.of(event)}")
public record LooseEvent(String id) {
}
