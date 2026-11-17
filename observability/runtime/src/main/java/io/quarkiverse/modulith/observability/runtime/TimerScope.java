package io.quarkiverse.modulith.observability.runtime;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkiverse.modulith.observability.ModuleInvocation;
import io.quarkiverse.modulith.observability.ModulithObservations;

/**
 * A running timer of a module invocation, tagged with the error, if any, like Micrometer's observation handler does.
 */
final class TimerScope implements ObservationScope {

    private final MeterRegistry registry;
    private final ModuleInvocation invocation;
    private final Timer.Sample sample;
    private String error = "none";

    TimerScope(MeterRegistry registry, ModuleInvocation invocation, Timer.Sample sample) {
        this.registry = registry;
        this.invocation = invocation;
        this.sample = sample;
    }

    @Override
    public void error(Throwable error) {
        this.error = error.getClass().getSimpleName();
    }

    @Override
    public void stop() {
        sample.stop(Timer.builder(ModulithObservations.MODULE_INVOCATIONS)
                .tag(ModulithObservations.MODULE_IDENTIFIER, invocation.moduleIdentifier())
                .tag(ModulithObservations.MODULE_NAME, invocation.moduleName())
                .tag(ModulithObservations.INVOCATION_TYPE, invocation.invocationType())
                .tag("error", error)
                .register(registry));
    }
}
