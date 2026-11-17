package io.quarkiverse.modulith.events.neo4j.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime settings of the Neo4j event publication registry, the counterpart of Spring Modulith's
 * `spring.modulith.events.neo4j.*` properties.
 */
@ConfigMapping(prefix = "quarkus.modulith.events.neo4j")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface Neo4jConfig {

    /**
     * The Neo4j database of the publication nodes. The default database of the server when not set.
     */
    Optional<String> database();

    /**
     * The index on the event hash.
     */
    EventIndex eventIndex();

    /**
     * The index on the event hash.
     */
    interface EventIndex {

        /**
         * Whether to create, at startup and unless they exist, the `eventHashIndex` index on the `eventHash` property
         * of `Neo4jEventPublication` nodes and the unique constraints `eventPublicationIdentifier` and
         * `eventPublicationArchiveIdentifier` on the `identifier` property of `Neo4jEventPublication` and
         * `Neo4jEventPublicationArchive` nodes, which every lifecycle update looks publications up by.
         */
        @WithDefault("false")
        boolean enabled();
    }
}
