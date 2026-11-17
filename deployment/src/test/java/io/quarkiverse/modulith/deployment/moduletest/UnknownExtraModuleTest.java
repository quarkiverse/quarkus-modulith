package io.quarkiverse.modulith.deployment.moduletest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication;
import io.quarkus.test.QuarkusExtensionTest;

public class UnknownExtraModuleTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage()))
            .overrideConfigKey("quarkus.modulith.test.module", "inventory")
            .overrideConfigKey("quarkus.modulith.test.extra-modules", "order,billing")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .assertException(t -> assertThat(t).hasStackTraceContaining(
                    "quarkus.modulith.test.extra-modules names billing, which is not a module of"));

    @Test
    void failsTheBuild() {
    }
}
