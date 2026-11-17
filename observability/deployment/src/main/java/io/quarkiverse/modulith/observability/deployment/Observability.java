package io.quarkiverse.modulith.observability.deployment;

import java.util.Optional;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.runtime.metrics.MetricsFactory;

/**
 * Which parts of module observability are active, decided from the configuration and the extensions present.
 */
final class Observability {

    private Observability() {
    }

    static boolean tracing(ObservabilityConfig config, Capabilities capabilities) {
        return config.tracing().enabled() && capabilities.isPresent(Capability.OPENTELEMETRY_TRACER);
    }

    static boolean metrics(ObservabilityConfig config, Optional<MetricsCapabilityBuildItem> metrics) {
        return config.metrics().enabled()
                && metrics.filter(it -> it.metricsSupported(MetricsFactory.MICROMETER)).isPresent();
    }

}
