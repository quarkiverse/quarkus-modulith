package io.quarkiverse.modulith.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.Violation;
import io.quarkiverse.modulith.deployment.Failures;
import io.quarkus.test.QuarkusExtensionTest;

public class NotAllowedDependencyFailsBuildTest {

    static final String BASE = "io.quarkiverse.modulith.deployment.fixtures.notallowed";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, BASE))
            .overrideConfigKey("quarkus.modulith.base-package", BASE)
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .assertException(t -> assertThat(Failures.violations(t).orElseThrow(() -> new AssertionError(t))
                    .ofKind(Violation.Kind.DEPENDENCY_NOT_ALLOWED))
                    .extracting(Violation::message)
                    .containsExactlyInAnyOrder(
                            "Module 'billing' depends on module 'customer' via " + BASE + ".billing.Billing -> " + BASE
                                    + ".customer.Customer. Allowed targets: catalog.",
                            "Module 'billing' depends on named interface(s) 'catalog :: spi' via " + BASE
                                    + ".billing.Billing -> " + BASE + ".catalog.spi.PriceProvider. Allowed targets: catalog."));

    @Test
    void failsTheBuild() {
    }
}
