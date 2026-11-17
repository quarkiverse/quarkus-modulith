package io.quarkiverse.modulith.deployment.moduletest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.Violations;
import io.quarkus.test.QuarkusExtensionTest;

public class VerifyAutomaticallyFailsBuildTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, "io.quarkiverse.modulith.deployment.fixtures.cycle"))
            .overrideConfigKey("quarkus.modulith.base-package", "io.quarkiverse.modulith.deployment.fixtures.cycle")
            .overrideConfigKey("quarkus.modulith.verification.mode", "warn")
            .overrideConfigKey("quarkus.modulith.test.module", "a")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .assertException(t -> assertThat(t).hasStackTraceContaining(Violations.class.getName())
                    .hasStackTraceContaining("Cycle detected: a -> b -> a"));

    @Test
    void failsTheBuild() {
    }
}
