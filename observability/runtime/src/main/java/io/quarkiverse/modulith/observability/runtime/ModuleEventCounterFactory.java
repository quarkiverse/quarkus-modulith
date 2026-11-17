package io.quarkiverse.modulith.observability.runtime;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Counter;
import io.quarkiverse.modulith.observability.ModulithEventMetrics;
import io.quarkiverse.modulith.observability.ModulithEventMetricsCustomizer;
import io.quarkus.arc.All;

/**
 * Creates the per event type counters, applying the {@link ModulithEventMetricsCustomizer} beans. Spring Modulith's
 * {@code ModuleEventCounterFactory}.
 */
@Singleton
public class ModuleEventCounterFactory implements ModulithEventMetrics {

    private final SortedSet<CounterCreator> creators = new TreeSet<>();
    private final SortedSet<CounterAugmenter> customizers = new TreeSet<>();

    @Inject
    ModuleEventCounterFactory(@All List<ModulithEventMetricsCustomizer> customizers) {
        customizers.forEach(it -> it.customize(this));
    }

    Counter.Builder createCounterBuilder(ObservedModuleEvent event) {
        Counter.Builder builder = creators.stream()
                .filter(it -> it.supports(event))
                .findFirst()
                .map(it -> it.creator().apply(event))
                .orElseGet(event::createBuilder);
        for (CounterAugmenter customizer : customizers.reversed()) {
            if (customizer.supports(event)) {
                builder = customizer.augmenter().apply(event.event(), builder);
            }
        }
        return builder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ModulithEventMetrics customize(Class<T> type, Function<T, Counter.Builder> factory) {
        creators.add(new CounterCreator(type, event -> factory.apply((T) event.event())));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ModulithEventMetrics customize(Class<T> type, BiConsumer<T, Counter.Builder> consumer) {
        customizers.add(new CounterAugmenter(type, (event, builder) -> {
            consumer.accept((T) event, builder);
            return builder;
        }));
        return this;
    }
}
