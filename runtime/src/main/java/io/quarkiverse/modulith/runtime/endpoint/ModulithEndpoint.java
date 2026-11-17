package io.quarkiverse.modulith.runtime.endpoint;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * The handler of {@code /q/modulith}, as a non-generic type so it can be a startup service on Quarkus 4.
 */
public interface ModulithEndpoint extends Handler<RoutingContext> {
}
