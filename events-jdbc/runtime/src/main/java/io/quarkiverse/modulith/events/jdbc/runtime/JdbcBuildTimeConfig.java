package io.quarkiverse.modulith.events.jdbc.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Settings of the JDBC event publication registry that are fixed when the application is built.
 */
@ConfigMapping(prefix = "quarkus.modulith.events.jdbc")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface JdbcBuildTimeConfig {

    /**
     * Whether the tables have Spring Modulith's legacy structure (version 1: no `STATUS`, `COMPLETION_ATTEMPTS` and
     * `LAST_RESUBMISSION_DATE` columns). Publications in a legacy table cannot be marked processing or failed. Takes
     * effect at build time, because it selects the repository bean.
     */
    @WithDefault("false")
    boolean useLegacyStructure();

    /**
     * The name of the datasource that holds the event publication tables. The default datasource when not set. Takes
     * effect at build time.
     */
    Optional<String> datasource();
}
