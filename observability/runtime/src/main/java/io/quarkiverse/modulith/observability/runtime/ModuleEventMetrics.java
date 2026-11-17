package io.quarkiverse.modulith.observability.runtime;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.observability.ModulithMetrics;

/**
 * Counts module events, Spring Modulith's {@code ModuleEventListener}: {@code module.events.published} for all events
 * and {@code module.events.published.<module>.<SimpleTypeName>} per event type. A failure to count, for example in a
 * {@code ModulithEventMetricsCustomizer} of the application, is logged and never fails the publication of the event.
 */
@Singleton
public class ModuleEventMetrics {

    private static final FailureLog FAILURES = new FailureLog(ModuleEventMetrics.class);

    private final Map<Class<?>, Optional<ApplicationModule>> modulesByType = new ConcurrentHashMap<>();

    @Inject
    MeterRegistry registry;

    @Inject
    ApplicationModules modules;

    @Inject
    ModuleEventCounterFactory factory;

    public void record(Object event) {
        Optional<ApplicationModule> module = modulesByType.computeIfAbsent(event.getClass(),
                modules::getModuleForType);
        if (module.isEmpty()) {
            return;
        }
        try {
            count(module.get(), event);
        } catch (RuntimeException e) {
            FAILURES.failed(event.getClass().getName(), "Cannot count the module event " + event.getClass().getName(), e);
        }
    }

    @PreDestroy
    void stop() {
        ModuleEventObserver.forget();
    }

    private void count(ApplicationModule module, Object event) {
        ObservedModuleEvent moduleEvent = new ObservedModuleEvent(module.getName(), event);
        Counter.Builder overall = Counter.builder(ModulithMetrics.ALL_EVENTS)
                .tag(ModulithMetrics.EVENT_TYPE, moduleEvent.eventReference());
        Stream.of(overall, factory.createCounterBuilder(moduleEvent))
                .forEach(it -> it.tag(ModulithMetrics.MODULE_IDENTIFIER, module.getName())
                        .tag(ModulithMetrics.MODULE_NAME, module.getDisplayName())
                        .register(registry).increment());
    }
}
