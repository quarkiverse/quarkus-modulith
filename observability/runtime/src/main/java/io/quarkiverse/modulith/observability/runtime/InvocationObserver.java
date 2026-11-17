package io.quarkiverse.modulith.observability.runtime;

import io.quarkiverse.modulith.observability.ModuleInvocation;

/**
 * Observes module invocations: spans with OpenTelemetry, timers with Micrometer. The build registers one
 * implementation per library present.
 */
public interface InvocationObserver {

    ObservationScope start(ModuleInvocation invocation);
}
