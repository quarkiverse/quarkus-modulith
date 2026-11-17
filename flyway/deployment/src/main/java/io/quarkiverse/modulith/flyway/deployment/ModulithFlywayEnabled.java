package io.quarkiverse.modulith.flyway.deployment;

import java.util.function.BooleanSupplier;

/**
 * Whether module-aware migrations are on.
 */
class ModulithFlywayEnabled implements BooleanSupplier {

    private final ModulithFlywayConfig config;

    ModulithFlywayEnabled(ModulithFlywayConfig config) {
        this.config = config;
    }

    @Override
    public boolean getAsBoolean() {
        return config.enabled();
    }
}
