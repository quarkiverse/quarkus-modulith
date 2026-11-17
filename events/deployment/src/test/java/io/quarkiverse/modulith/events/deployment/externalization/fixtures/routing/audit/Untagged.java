package io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("untagged::#{@tags.refuse(#this)}")
public record Untagged(String name) {
}
