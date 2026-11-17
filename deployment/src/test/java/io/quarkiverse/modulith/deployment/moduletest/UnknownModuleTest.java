package io.quarkiverse.modulith.deployment.moduletest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication;
import io.quarkus.test.QuarkusExtensionTest;

public class UnknownModuleTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ShopApplication.class.getPackage()))
            .overrideConfigKey("quarkus.modulith.test.module", "billing")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .assertException(t -> assertThat(t).hasStackTraceContaining(
                    "quarkus.modulith.test.module=billing does not match any module of"));

    @Test
    void failsTheBuild() {
    }
}
