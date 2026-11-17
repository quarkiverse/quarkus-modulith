package io.quarkiverse.modulith.events.deployment.externalization.fixtures.invalid;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("#{T(java.util.UUID).randomUUID()}")
public record WorseEvent(String id) {
}
