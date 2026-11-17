package io.quarkiverse.modulith.deployment.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.Failures;
import io.quarkus.test.QuarkusExtensionTest;

public class InvalidRuleConfigTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ExplicitlyAnnotatedConfigTest.CONFIG))
            .overrideConfigKey("quarkus.modulith.base-package", ExplicitlyAnnotatedConfigTest.CONFIG)
            .overrideConfigKey("quarkus.modulith.verification.additional-rules", "java.lang.String")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .assertException(t -> assertThat(Failures.messages(t)).contains("quarkus.modulith.verification.additional-rules"
                    + " lists java.lang.String, which is not a ModulithRule with a public no-argument constructor"));

    @Test
    void failsTheBuild() {
    }
}
