package io.quarkiverse.modulith.observability;

import java.util.function.BiConsumer;
import java.util.function.Function;

import io.micrometer.core.instrument.Counter;

/**
 * Customizes the per event type counters, Spring Modulith's {@code ModulithEventMetrics}.
 */
public interface ModulithEventMetrics {

    /**
     * Adds tags to the counters of events of the given type, for example from the event's state.
     */
    <T> ModulithEventMetrics customize(Class<T> type, BiConsumer<T, Counter.Builder> consumer);

    /**
     * Creates the counter of events of the given type, replacing the default {@code module.events.published.*}.
     */
    <T> ModulithEventMetrics customize(Class<T> type, Function<T, Counter.Builder> factory);
}
