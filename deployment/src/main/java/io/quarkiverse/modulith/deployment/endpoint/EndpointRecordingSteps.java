package io.quarkiverse.modulith.deployment.endpoint;

import java.util.List;

import io.quarkiverse.modulith.deployment.docs.DocumentationModelBuildItem;
import io.quarkiverse.modulith.docs.ApplicationModulesJson;
import io.quarkiverse.modulith.runtime.endpoint.ModulithEndpoint;
import io.quarkiverse.modulith.runtime.endpoint.ModulithEndpointHandler;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.vertx.http.deployment.spi.RouteBuildItem;

/**
 * Registers {@code /q/modulith} with a handler created by a static-init service.
 */
class EndpointRecordingSteps {

    @BuildStep(onlyIf = EndpointEnabled.class)
    void route(ServiceRegistrar services, DocumentationModelBuildItem documentation, EndpointConfig config,
            Capabilities capabilities, BuildProducer<RouteBuildItem> routes) {
        if (!Endpoints.hasHttp(capabilities)) {
            return;
        }
        List<String> json = List.copyOf(ModulithEndpointHandler.parts(ApplicationModulesJson.simple(documentation.getModel())));
        services.forService(ModulithEndpoint.class)
                .atPhase(Phase.STATIC_INIT)
                .onStart(context -> ModulithEndpointHandler.of(json));
        routes.produce(Endpoints.route(config, services.getRecorderProxy(ModulithEndpoint.class)));
    }
}
