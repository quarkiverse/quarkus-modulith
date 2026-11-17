package io.quarkiverse.modulith.events.neo4j.deployment;

import java.util.List;

import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

/**
 * What the Neo4j extension needs on this Quarkus line. The Quarkiverse Neo4j extension initializes Netty's TLS
 * package at run time, and on Vert.x 4 also the Vert.x class that maps client authentication modes to Netty's
 * {@code ClientAuth}. Vert.x 5 moved that mapping into a static map of {@code SslContextManager}, which Quarkus 4
 * initializes at build time, so a native build fails with a {@code ClientAuth} object in the image heap. This defers
 * {@code SslContextManager} too.
 */
final class Neo4jLine {

    static final String SSL_CONTEXT_MANAGER = "io.vertx.core.internal.tls.SslContextManager";

    private Neo4jLine() {
    }

    static List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses() {
        return List.of(new RuntimeInitializedClassBuildItem(SSL_CONTEXT_MANAGER));
    }
}
