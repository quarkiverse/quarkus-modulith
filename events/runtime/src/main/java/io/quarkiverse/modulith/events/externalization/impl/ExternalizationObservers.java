package io.quarkiverse.modulith.events.externalization.impl;

import io.quarkiverse.modulith.events.externalization.ExternalizationStrategy;
import io.quarkus.arc.Arc;

/**
 * Called by the observers the build registers for the event types the application publishes.
 */
public final class ExternalizationObservers {

    private ExternalizationObservers() {
    }

    /**
     * Hands the event to the externalization strategy.
     */
    public static void notify(Object event) {
        Arc.container().instance(ExternalizationStrategy.class).get().onEvent(event);
    }
}
