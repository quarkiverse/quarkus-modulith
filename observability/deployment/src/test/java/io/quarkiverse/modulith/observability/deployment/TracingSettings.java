package io.quarkiverse.modulith.observability.deployment;

/**
 * {@link ObservabilityConfig.Tracing} values for tests.
 */
record TracingSettings(boolean enabled) implements ObservabilityConfig.Tracing {
}
