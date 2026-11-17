package io.quarkiverse.modulith.deployment.devui;

import java.util.Optional;

import io.quarkiverse.modulith.runtime.ModulithCapabilities;
import io.quarkiverse.modulith.runtime.dev.ModulithJsonRpcService;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;

/**
 * Registers the JSON-RPC backend of the Dev UI publications page. Quarkus 3 variant, the Quarkus 4 one uses the build item of the Dev JSON-RPC SPI.
 */
class ModulithJsonRpcSteps {

    /**
     * Registers the backend when an event publication store is present.
     */
    @BuildStep(onlyIf = IsLocalDevelopment.class)
    Optional<JsonRPCProvidersBuildItem> jsonRpc(Capabilities capabilities) {
        return capabilities.isPresent(ModulithCapabilities.EVENT_PUBLICATION_REPOSITORY)
                ? Optional.of(new JsonRPCProvidersBuildItem(ModulithJsonRpcService.class))
                : Optional.empty();
    }
}
