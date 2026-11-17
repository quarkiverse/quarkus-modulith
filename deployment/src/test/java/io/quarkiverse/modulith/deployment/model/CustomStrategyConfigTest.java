package io.quarkiverse.modulith.deployment.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkus.test.QuarkusExtensionTest;

public class CustomStrategyConfigTest {

    static final String CONFIG = ExplicitlyAnnotatedConfigTest.CONFIG;

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, CONFIG).addClass(SecondOnlyStrategy.class))
            .overrideConfigKey("quarkus.modulith.base-package", CONFIG)
            .overrideConfigKey("quarkus.modulith.detection-strategy", SecondOnlyStrategy.class.getName())
            .overrideConfigKey("quarkus.modulith.verification.mode", "disabled")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    ApplicationModules modules;

    @Test
    void usesTheConfiguredStrategyClass() {
        // act
        var names = modules.stream().map(ApplicationModule::getName).toList();

        // assert
        assertThat(names).containsExactly("second");
        assertThat(modules.getModelData().verified()).isFalse();
        assertThat(modules.getModelData().mermaid()).startsWith("flowchart LR");
    }
}
