package io.quarkiverse.modulith.events.mongodb.runtime;

import org.bson.UuidRepresentation;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime settings of the MongoDB event publication registry.
 */
@ConfigMapping(prefix = "quarkus.modulith.events.mongodb")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface MongoDbConfig {

    /**
     * The database of the `event_publication` and `event_publication_archive` collections. Defaults to
     * `quarkus.mongodb.database`, the database MongoDB with Panache uses.
     */
    @WithDefault("${quarkus.mongodb.database:modulith}")
    String database();

    /**
     * How publication identifiers are stored. `standard` (binary subtype 4) by default. Use `java-legacy` to read
     * collections written by Spring Boot applications that kept Spring Boot's default.
     */
    @WithDefault("standard")
    UuidRepresentation uuidRepresentation();
}
