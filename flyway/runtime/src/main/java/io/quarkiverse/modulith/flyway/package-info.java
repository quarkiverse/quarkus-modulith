/**
 * Module-aware Flyway migrations, like Spring Modulith's {@code spring.modulith.runtime.flyway-enabled}: the
 * migrations in the {@code __root} folder of each location run first with the configured history table, then the
 * migrations in the folder of each module, in module dependency order, each with its own history table, the configured
 * name plus an underscore and the module name. Implement {@link io.quarkiverse.modulith.flyway.MigrationFilter} to skip
 * modules.
 */
package io.quarkiverse.modulith.flyway;
