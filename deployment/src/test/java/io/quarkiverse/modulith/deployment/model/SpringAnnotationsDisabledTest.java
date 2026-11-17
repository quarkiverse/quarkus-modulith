package io.quarkiverse.modulith.deployment.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkus.test.QuarkusExtensionTest;

public class SpringAnnotationsDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, SpringAnnotationsBuildTest.SPRING))
            .overrideConfigKey("quarkus.modulith.spring-annotations.enabled", "false")
            .overrideConfigKey("quarkus.modulith.base-package", SpringAnnotationsBuildTest.SPRING)
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    ApplicationModules modules;

    @Test
    void ignoresSpringModulithDeclarations() {
        // act
        var closed = modules.getModuleByName("closed").orElseThrow();

        // assert
        assertThat(closed.getAllowedDependencies()).isEmpty();
        assertThat(modules.getSystemName()).isEmpty();
        assertThat(modules.stream().map(ApplicationModule::getName)).containsExactly("target", "closed");
    }
}
