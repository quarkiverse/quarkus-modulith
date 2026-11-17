package io.quarkiverse.modulith.deployment.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.Failures;
import io.quarkus.test.QuarkusExtensionTest;

public class SpringAnnotationsBuildTest {

    static final String SPRING = "io.quarkiverse.modulith.deployment.fixtures.model.spring";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, SPRING))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .assertException(t -> assertThat(Failures.messages(t)).contains("- Module 'closed' depends on module"
                    + " 'target' via " + SPRING + ".closed.Closed -> " + SPRING + ".target.Target. Allowed targets: none."));

    @Test
    void honorsSpringModulithDeclarations() {
    }
}
