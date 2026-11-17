package io.quarkiverse.modulith.events.jdbc.deployment;

import java.util.Optional;

import io.quarkiverse.modulith.events.jdbc.runtime.JdbcBuildTimeConfig;

/**
 * Build-time settings for calling the build steps directly.
 */
public record FixedJdbcBuildTimeConfig(boolean useLegacyStructure, Optional<String> datasource)
        implements
            JdbcBuildTimeConfig {
}
