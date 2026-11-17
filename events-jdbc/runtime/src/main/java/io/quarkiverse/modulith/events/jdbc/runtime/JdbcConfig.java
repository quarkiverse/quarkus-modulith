package io.quarkiverse.modulith.events.jdbc.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime settings of the JDBC event publication registry, the counterpart of Spring Modulith's
 * `spring.modulith.events.jdbc.*` properties.
 */
@ConfigMapping(prefix = "quarkus.modulith.events.jdbc")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface JdbcConfig {

    /**
     * The schema of the event publication tables. The connection's default schema when not set. Only H2, HSQLDB and
     * PostgreSQL support it.
     */
    Optional<String> schema();

    /**
     * Creation of the event publication tables at startup.
     */
    SchemaInitialization schemaInitialization();

    /**
     * Creation of the event publication tables at startup.
     */
    interface SchemaInitialization {

        /**
         * Whether to create the event publication table (and the archive table in `archive` completion mode) at startup
         * when it does not exist. An existing table is left as it is. Turn it off when Flyway or Liquibase manage the
         * tables.
         */
        @WithDefault("false")
        boolean enabled();
    }
}
