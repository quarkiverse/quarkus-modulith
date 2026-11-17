package io.quarkiverse.modulith.events.fixtures.app.order;

import io.quarkiverse.modulith.events.Externalized;

@Externalized(target = "alias-target", value = "ignored")
public record TargetAlias(String id) {
}
