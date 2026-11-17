package io.quarkiverse.modulith.deployment.endpoint;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.vertx.http.deployment.spi.RouteBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Declares the {@code /q/modulith} route around a recorded handler.
 */
public final class Endpoints {

    static final String TITLE = "Application modules";

    private Endpoints() {
    }

    /**
     * @return whether the application has an HTTP server to serve the endpoint
     */
    public static boolean hasHttp(Capabilities capabilities) {
        return capabilities.isPresent(Capability.VERTX_HTTP);
    }

    public static RouteBuildItem route(EndpointConfig config, Handler<RoutingContext> handler) {
        return RouteBuildItem.newManagementRoute(config.path())
                .withRequestHandler(handler)
                .displayOnNotFoundPage(TITLE)
                .build();
    }
}
