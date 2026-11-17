package io.quarkiverse.modulith.observability.deployment;

/**
 * {@link ObservabilityConfig} values for tests that call build steps directly.
 */
record ObservationSettings(boolean enabled, Tracing tracing, Metrics metrics) implements ObservabilityConfig {

    static ObservationSettings of(boolean tracing, boolean metrics) {
        return new ObservationSettings(true, new TracingSettings(tracing), new MetricsSettings(metrics));
    }
}
