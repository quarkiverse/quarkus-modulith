package io.quarkiverse.modulith.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.Failures;
import io.quarkus.test.QuarkusExtensionTest;

public class InvalidListenerTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, "io.quarkiverse.modulith.deployment.fixtures.badlistener"))
            .overrideConfigKey("quarkus.modulith.base-package", "io.quarkiverse.modulith.deployment.fixtures.badlistener")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .assertException(t -> assertThat(Failures.messages(t))
                    .contains("@ApplicationModuleListener method must declare exactly one parameter, the event")
                    .contains("BadListener#on"));

    @Test
    void failsTheBuild() {
    }
}
