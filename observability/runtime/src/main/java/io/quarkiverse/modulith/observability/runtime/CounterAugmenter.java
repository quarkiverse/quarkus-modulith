package io.quarkiverse.modulith.observability.runtime;

import java.util.function.BiFunction;

import io.micrometer.core.instrument.Counter;

/**
 * Adds to the counter of events of a type, for example tags from the event's state.
 */
record CounterAugmenter(Class<?> type, BiFunction<Object, Counter.Builder, Counter.Builder> augmenter)
        implements
            Comparable<CounterAugmenter> {

    boolean supports(ObservedModuleEvent event) {
        return type.isInstance(event.event());
    }

    @Override
    public int compareTo(CounterAugmenter that) {
        return TypeSpecificity.compare(type, that.type);
    }
}
