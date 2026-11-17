package io.quarkiverse.modulith.observability.deployment;

/**
 * {@link ObservabilityConfig.Metrics} values for tests.
 */
record MetricsSettings(boolean enabled) implements ObservabilityConfig.Metrics {
}
