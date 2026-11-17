package io.quarkiverse.modulith.deployment.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkus.test.QuarkusExtensionTest;

public class ExplicitlyAnnotatedConfigTest {

    static final String CONFIG = "io.quarkiverse.modulith.deployment.fixtures.model.config";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, CONFIG))
            .overrideConfigKey("quarkus.modulith.base-package", CONFIG)
            .overrideConfigKey("quarkus.modulith.detection-strategy", "explicitly-annotated")
            .overrideConfigKey("quarkus.modulith.system-name", "Configured")
            .overrideConfigKey("quarkus.modulith.use-fully-qualified-module-names", "true")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    ApplicationModules modules;

    @Test
    void detectsOnlyAnnotatedPackagesWithFullNames() {
        // act
        var names = modules.stream().map(ApplicationModule::getName).toList();

        // assert
        assertThat(names).containsExactly(CONFIG + ".first");
        assertThat(modules.getSystemName()).isEqualTo("Configured");
        assertThat(modules.getMetadata().useFullyQualifiedModuleNames()).isTrue();
        assertThat(modules.getModuleByName(CONFIG + ".first").map(ApplicationModule::getDisplayName)).hasValue("First one");
    }
}
