package io.quarkiverse.modulith.events.neo4j.deployment;

import java.util.List;

import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

/**
 * What the Neo4j extension needs on this Quarkus line. On Quarkus 3 the Quarkiverse Neo4j extension already defers
 * the Vert.x TLS helper to run time, so nothing is added.
 */
final class Neo4jLine {

    private Neo4jLine() {
    }

    static List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses() {
        return List.of();
    }
}
