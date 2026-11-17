package io.quarkiverse.modulith.events.deployment.externalization;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The externalized types and the beans their routing expressions call.
 */
public final class ExternalizationExpressionsBuildItem extends SimpleBuildItem {

    private final Set<String> beanNames;
    private final Set<String> externalizedTypes;

    public ExternalizationExpressionsBuildItem(Set<String> beanNames, Set<String> externalizedTypes) {
        this.beanNames = Set.copyOf(beanNames);
        this.externalizedTypes = Set.copyOf(externalizedTypes);
    }

    public Set<String> getBeanNames() {
        return beanNames;
    }

    public Set<String> getExternalizedTypes() {
        return externalizedTypes;
    }
}
