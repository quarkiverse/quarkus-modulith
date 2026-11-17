package io.quarkiverse.modulith.observability.deployment;

import java.util.function.BooleanSupplier;

/**
 * Whether {@code quarkus.modulith.observability.enabled} is set.
 */
public final class ObservabilityEnabled implements BooleanSupplier {

    private final ObservabilityConfig config;

    ObservabilityEnabled(ObservabilityConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled();
    }
}
