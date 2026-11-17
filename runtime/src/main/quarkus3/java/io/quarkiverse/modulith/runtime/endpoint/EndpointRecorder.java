package io.quarkiverse.modulith.runtime.endpoint;

import java.util.List;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Creates the handler of {@code /q/modulith}. Quarkus 3 only, Quarkus 4 creates it in a static-init service.
 */
@Recorder
public class EndpointRecorder {

    public Handler<RoutingContext> handler(List<String> json) {
        return ModulithEndpointHandler.of(json);
    }
}
