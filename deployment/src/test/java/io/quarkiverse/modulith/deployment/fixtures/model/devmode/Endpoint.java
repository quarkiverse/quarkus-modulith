package io.quarkiverse.modulith.deployment.fixtures.model.devmode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.vertx.ext.web.Router;

/**
 * Answers with the events the billing module listens to.
 */
@ApplicationScoped
public class Endpoint {

    @Inject
    ApplicationModules modules;

    void routes(@Observes Router router) {
        router.get("/listened").handler(it -> it.end(modules.getModuleByName("billing").orElseThrow()
                .getListenedEvents().toString()));
    }
}
