package io.quarkiverse.modulith.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Without {@code @Modulith} and without {@code quarkus.modulith.base-package} the base package is the common package
 * of the application classes. The test class is part of the application, so the common package is the one above it and
 * the fixtures. With verification disabled, a model with violations starts.
 */
public class InferredBasePackageTest {

    static final String GRANTS = "io.quarkiverse.modulith.deployment.fixtures.grants";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, GRANTS))
            .overrideConfigKey("quarkus.modulith.verification.mode", "disabled")
            .overrideConfigKey("quarkus.modulith.verification.include-test-classes", "true")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    ApplicationModules modules;

    @Test
    void infersThePackageCommonToTheFixturesAndTheTestClass() {
        // act
        String basePackage = modules.getBasePackage();

        // assert
        assertThat(basePackage).isEqualTo("io.quarkiverse.modulith.deployment");
        assertThat(modules.getModuleByName("fixtures").map(ApplicationModule::getBasePackage))
                .hasValue("io.quarkiverse.modulith.deployment.fixtures");
    }

    @Test
    void skipsTheVerification() {
        // act
        boolean verified = modules.getModelData().verified();

        // assert
        assertThat(verified).isFalse();
    }
}
