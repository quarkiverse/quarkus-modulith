package io.quarkiverse.modulith.observability.runtime;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkiverse.modulith.observability.ModuleInvocation;

/**
 * Times module invocations with the {@code module.invocations} timer Spring Modulith's observation records.
 */
@Singleton
public class TimingObserver implements InvocationObserver {

    @Inject
    MeterRegistry registry;

    @Override
    public ObservationScope start(ModuleInvocation invocation) {
        return new TimerScope(registry, invocation, Timer.start(registry));
    }
}
