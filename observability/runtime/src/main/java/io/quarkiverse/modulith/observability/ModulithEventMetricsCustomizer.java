package io.quarkiverse.modulith.observability;

/**
 * A CDI bean that customizes the event counters at startup, Spring Modulith's {@code ModulithEventMetricsCustomizer}.
 */
@FunctionalInterface
public interface ModulithEventMetricsCustomizer {

    void customize(ModulithEventMetrics metrics);
}
