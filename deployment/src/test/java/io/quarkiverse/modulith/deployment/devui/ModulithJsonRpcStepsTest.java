package io.quarkiverse.modulith.deployment.devui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.runtime.ModulithCapabilities;
import io.quarkiverse.modulith.runtime.dev.ModulithJsonRpcService;
import io.quarkus.deployment.Capabilities;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;

class ModulithJsonRpcStepsTest {

    @Test
    void registersTheJsonRpcBackendWhenAPublicationStoreExists() {
        // act
        Optional<JsonRPCProvidersBuildItem> jsonRpc = new ModulithJsonRpcSteps()
                .jsonRpc(new Capabilities(Set.of(ModulithCapabilities.EVENT_PUBLICATION_REPOSITORY)));

        // assert
        assertThat(jsonRpc).map(JsonRPCProvidersBuildItem::getJsonRPCMethodProviderClass)
                .contains(ModulithJsonRpcService.class);
    }

    @Test
    void registersNoJsonRpcBackendWithoutPublicationStore() {
        // act
        Optional<JsonRPCProvidersBuildItem> jsonRpc = new ModulithJsonRpcSteps().jsonRpc(new Capabilities(Set.of()));

        // assert
        assertThat(jsonRpc).isEmpty();
    }
}
