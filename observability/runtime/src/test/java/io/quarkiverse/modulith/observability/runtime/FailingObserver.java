package io.quarkiverse.modulith.observability.runtime;

import io.quarkiverse.modulith.observability.ModuleInvocation;

/**
 * Fails to start an observation, like a throwing span customizer, or starts one that fails to record and to stop.
 */
record FailingObserver(boolean failsToStart) implements InvocationObserver {

    @Override
    public ObservationScope start(ModuleInvocation invocation) {
        if (failsToStart) {
            throw new IllegalStateException("cannot start");
        }
        return new FailingScope();
    }
}
