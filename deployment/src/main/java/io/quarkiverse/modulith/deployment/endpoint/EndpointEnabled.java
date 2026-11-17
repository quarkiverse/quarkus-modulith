package io.quarkiverse.modulith.deployment.endpoint;

import java.util.function.BooleanSupplier;

import io.quarkus.runtime.LaunchMode;

/**
 * Whether {@code /q/modulith} is served: as {@code quarkus.modulith.endpoint.enabled} says, and in dev and test mode
 * only when it is not set.
 */
public final class EndpointEnabled implements BooleanSupplier {

    private final EndpointConfig config;
    private final LaunchMode launchMode;

    EndpointEnabled(EndpointConfig config, LaunchMode launchMode) {
        this.config = config;
        this.launchMode = launchMode;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled().orElse(launchMode.isDevOrTest());
    }
}
