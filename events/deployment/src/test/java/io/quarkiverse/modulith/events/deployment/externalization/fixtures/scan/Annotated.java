package io.quarkiverse.modulith.events.deployment.externalization.fixtures.scan;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("a::{event.id}")
public record Annotated(String id) {
}
