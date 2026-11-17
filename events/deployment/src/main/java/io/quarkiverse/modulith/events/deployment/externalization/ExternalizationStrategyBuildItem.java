package io.quarkiverse.modulith.events.deployment.externalization;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Produced by an extension that brings its own outbox, such as {@code quarkus-modulith-events-jobrunr}, to replace the
 * outbox on the event publication registry. The bean class implements {@code ExternalizationStrategy} and is
 * registered by the extension that produces this item.
 */
public final class ExternalizationStrategyBuildItem extends SimpleBuildItem {

    private final String name;

    public ExternalizationStrategyBuildItem(String name) {
        this.name = name;
    }

    /**
     * @return a name of the outbox for messages, such as {@code JobRunr}
     */
    public String getName() {
        return name;
    }
}
