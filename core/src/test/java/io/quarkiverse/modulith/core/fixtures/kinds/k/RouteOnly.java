package io.quarkiverse.modulith.core.fixtures.kinds.k;

public class RouteOnly {
    @io.quarkus.vertx.web.Route(path = "/r", methods = "GET")
    void route() {
    }

    void onApplicationEvent(Object event) {
    }
}
