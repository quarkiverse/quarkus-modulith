package io.quarkiverse.modulith.flyway.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Module-aware Flyway migrations.
 */
@ConfigMapping(prefix = "quarkus.modulith.flyway")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ModulithFlywayConfig {

    /**
     * Whether to apply module-specific Flyway migrations to the default datasource, the counterpart of
     * `spring.modulith.runtime.flyway-enabled`. The migrations in the `__root` folder of each Flyway location run
     * first with the configured history table, then, in module dependency order, those in the folder named after each
     * module, with a history table named after the configured one plus an underscore and the module name (for example
     * `flyway_schema_history_order`). In a module test only the modules the test starts are migrated. Takes effect at
     * build time.
     */
    @WithDefault("false")
    boolean enabled();
}
