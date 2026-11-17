package io.quarkiverse.modulith.events.deployment.externalization.fixtures.routing.audit;

import io.quarkiverse.modulith.events.Externalized;

@Externalized("{event.label.name}::#{@tags.tag(#this)}")
public record Tagged(Label label) {
}
