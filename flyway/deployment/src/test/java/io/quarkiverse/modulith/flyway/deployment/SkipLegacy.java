package io.quarkiverse.modulith.flyway.deployment;

import jakarta.inject.Singleton;

import org.flywaydb.core.Flyway;

import io.quarkiverse.modulith.core.ApplicationModuleIdentifier;
import io.quarkiverse.modulith.flyway.MigrationFilter;

/**
 * Skips the migrations of the legacy module, and checks the Flyway instance it gets is the module's.
 */
@Singleton
public class SkipLegacy implements MigrationFilter {

    @Override
    public boolean shouldMigrate(ApplicationModuleIdentifier module, Flyway flyway) {
        return !module.value().equals("legacy")
                && flyway.getConfiguration().getTable().equals("flyway_schema_history_" + module.value());
    }
}
