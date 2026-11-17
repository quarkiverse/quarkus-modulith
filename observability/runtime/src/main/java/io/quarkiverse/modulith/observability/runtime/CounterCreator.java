package io.quarkiverse.modulith.observability.runtime;

import java.util.function.Function;

import io.micrometer.core.instrument.Counter;

/**
 * Creates the counter of events of a type instead of the default one.
 */
record CounterCreator(Class<?> type, Function<ObservedModuleEvent, Counter.Builder> creator)
        implements
            Comparable<CounterCreator> {

    boolean supports(ObservedModuleEvent event) {
        return type.isInstance(event.event());
    }

    @Override
    public int compareTo(CounterCreator that) {
        return TypeSpecificity.compare(type, that.type);
    }
}
