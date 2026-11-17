package io.quarkiverse.modulith.moments.deployment;

import java.util.function.BooleanSupplier;

/**
 * Runs a build step only when {@code quarkus.modulith.moments.enabled} is {@code true}.
 */
class MomentsEnabled implements BooleanSupplier {

    MomentsBuildTimeConfig config;

    @Override
    public boolean getAsBoolean() {
        return config.enabled();
    }
}
