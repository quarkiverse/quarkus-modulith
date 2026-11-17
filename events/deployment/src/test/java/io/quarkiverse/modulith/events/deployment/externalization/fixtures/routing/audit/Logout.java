package io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("{event.user}-out::{inject:keys.of(event)}")
public record Logout(String user) {
}
