package io.quarkiverse.modulith.events.deployment.externalization.fixtures.invalid;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("bad::{event.missing}")
public record BadEvent(String id) {
}
