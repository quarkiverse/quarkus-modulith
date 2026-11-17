package io.quarkiverse.modulith.flyway;

import org.flywaydb.core.Flyway;

import io.quarkiverse.modulith.core.ApplicationModuleIdentifier;

/**
 * Decides whether the migrations of a module run, like Spring Modulith's {@code MigrationFilter}. Declare CDI beans of
 * this type: a module is migrated only when every filter agrees. Modules without migration files and modules left out
 * of a module test are never migrated.
 */
@FunctionalInterface
public interface MigrationFilter {

    /**
     * @param module the module
     * @param flyway the Flyway instance configured for the module's migration folder and history table
     * @return whether to run the module's migrations
     */
    boolean shouldMigrate(ApplicationModuleIdentifier module, Flyway flyway);
}
