package io.quarkiverse.modulith.deployment.moduletest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.cycle.a.A;
import io.quarkiverse.modulith.deployment.fixtures.cycle.b.B;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

public class VerifyAutomaticallyOffTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, "io.quarkiverse.modulith.deployment.fixtures.cycle")
                    .addClass(SetupResource.class))
            .overrideConfigKey("quarkus.modulith.base-package", "io.quarkiverse.modulith.deployment.fixtures.cycle")
            .overrideConfigKey("quarkus.modulith.verification.mode", "warn")
            .overrideConfigKey("quarkus.modulith.test.module", "a")
            .overrideConfigKey("quarkus.modulith.test.verify-automatically", "false")
            .overrideConfigKey("quarkus.arc.remove-unused-beans", "false")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Test
    void startsTheModuleTest() {
        // act
        String setup = SetupResource.read();

        // assert
        assertThat(setup).isEqualTo("module=a\nbootstrap-mode=STANDALONE\ndependencies=\nextra-includes=\n"
                + "shared-modules=\n");
        assertThat(Arc.container().instance(A.class).isAvailable()).isTrue();
        assertThat(Arc.container().instance(B.class).isAvailable()).isFalse();
    }
}
