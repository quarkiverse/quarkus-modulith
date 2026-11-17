package io.quarkiverse.modulith.deployment.moduletest;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication;
import io.quarkiverse.modulith.testing.ModuleTestRuntimeConfig;
import io.quarkus.test.QuarkusExtensionTest;

public class ModuleTestRuntimeConfigTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage()))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideRuntimeConfigKey("quarkus.modulith.test.thread-bound-published-events", "true");

    @Inject
    ModuleTestRuntimeConfig config;

    @Test
    void readsThreadBoundPublishedEventsAtRuntime() {
        // act
        boolean threadBound = config.threadBoundPublishedEvents();

        // assert
        assertThat(threadBound).isTrue();
    }
}
