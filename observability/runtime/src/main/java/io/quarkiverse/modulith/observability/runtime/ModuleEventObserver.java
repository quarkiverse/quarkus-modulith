package io.quarkiverse.modulith.observability.runtime;

import java.util.Objects;

import jakarta.enterprise.inject.spi.EventContext;

import io.quarkus.arc.Arc;

/**
 * Called by the observers the build registers for every module event type, so the counters see each event without
 * an observer of {@code Object}. The {@link ModuleEventMetrics} bean is looked up on the first event only, and
 * forgotten when the application stops, so a restarted application in dev mode looks up its own.
 */
public final class ModuleEventObserver {

    private static volatile ModuleEventMetrics metrics;

    private ModuleEventObserver() {
    }

    public static void notify(EventContext<?> context) {
        metrics().record(context.getEvent());
    }

    static void forget() {
        metrics = null;
    }

    private static ModuleEventMetrics metrics() {
        ModuleEventMetrics current = metrics;
        if (Objects.isNull(current)) {
            current = Arc.container().instance(ModuleEventMetrics.class).get();
            metrics = current;
        }
        return current;
    }
}
