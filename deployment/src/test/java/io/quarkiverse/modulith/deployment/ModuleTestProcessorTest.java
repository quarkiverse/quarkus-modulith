package io.quarkiverse.modulith.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.deployment.moduletest.FixedModuleTestConfig;
import io.quarkiverse.modulith.deployment.moduletest.ModuleTestBuildItem;
import io.quarkiverse.modulith.deployment.moduletest.ModuleTestConfig;
import io.quarkiverse.modulith.deployment.moduletest.ModuleTestConfig.BootstrapMode;

class ModuleTestProcessorTest {

    @ParameterizedTest
    @ValueSource(strings = { "order", "" })
    void startsAllModulesOutsideTests(String module) {
        // arrange
        ModuleTestConfig config = new FixedModuleTestConfig(Optional.of(module).filter(it -> !it.isBlank()),
                BootstrapMode.STANDALONE, Optional.empty(), true);

        // act
        ModuleTestBuildItem moduleTest = new ModuleTestProcessor().noModuleTest(config);

        // assert
        assertThat(moduleTest.getSetup()).isEmpty();
    }
}
