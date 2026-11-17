package io.quarkiverse.modulith.observability.runtime;

import io.micrometer.core.instrument.Counter;
import io.quarkiverse.modulith.observability.ModulithMetrics;

/**
 * An event published by a module.
 *
 * @param module the identifier of the module the event type belongs to
 * @param event the event
 */
record ObservedModuleEvent(String module, Object event) {

    Counter.Builder createBuilder() {
        return Counter.builder(ModulithMetrics.ALL_EVENTS + "." + eventReference());
    }

    /**
     * @return {@code <module>.<SimpleTypeName>}
     */
    String eventReference() {
        return module + "." + event.getClass().getSimpleName();
    }
}
