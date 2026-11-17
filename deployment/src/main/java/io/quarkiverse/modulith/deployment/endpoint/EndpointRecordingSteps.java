package io.quarkiverse.modulith.deployment.endpoint;

import io.quarkiverse.modulith.deployment.docs.DocumentationModelBuildItem;
import io.quarkiverse.modulith.docs.ApplicationModulesJson;
import io.quarkiverse.modulith.runtime.endpoint.EndpointRecorder;
import io.quarkiverse.modulith.runtime.endpoint.ModulithEndpointHandler;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.vertx.http.deployment.spi.RouteBuildItem;

/**
 * Registers {@code /q/modulith} with a handler from a bytecode recorder.
 */
class EndpointRecordingSteps {

    /**
     * Recorded steps always run, so the step checks itself that an HTTP server exists.
     */
    @BuildStep(onlyIf = EndpointEnabled.class)
    @Record(ExecutionTime.STATIC_INIT)
    void route(EndpointRecorder recorder, DocumentationModelBuildItem documentation, EndpointConfig config,
            Capabilities capabilities, BuildProducer<RouteBuildItem> routes) {
        if (Endpoints.hasHttp(capabilities)) {
            routes.produce(Endpoints.route(config,
                    recorder.handler(ModulithEndpointHandler.parts(ApplicationModulesJson.simple(documentation.getModel())))));
        }
    }
}
