package io.quarkiverse.modulith.events.deployment.externalization;

import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The event types externalization observes.
 */
public final class ObservedEventTypesBuildItem extends SimpleBuildItem {

    private final Set<DotName> types;

    public ObservedEventTypesBuildItem(Set<DotName> types) {
        this.types = Set.copyOf(types);
    }

    public Set<DotName> getTypes() {
        return types;
    }
}
