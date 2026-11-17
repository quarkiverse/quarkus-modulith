package io.quarkiverse.modulith.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.Violation;
import io.quarkiverse.modulith.deployment.Failures;
import io.quarkus.test.QuarkusExtensionTest;

public class InternalAccessFailsBuildTest {

    static final String BASE = "io.quarkiverse.modulith.deployment.fixtures.internalaccess";
    static final String HEADER = "Module 'order' depends on non-exposed type " + BASE
            + ".inventory.internal.Stock within module 'inventory'!" + System.lineSeparator();
    static final String ORIGIN = "Method <" + BASE + ".order.OrderService.reserve()> calls ";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, BASE))
            .overrideConfigKey("quarkus.modulith.base-package", BASE)
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .assertException(t -> assertThat(Failures.violations(t).orElseThrow(() -> new AssertionError(t))
                    .getViolations())
                    .containsExactlyInAnyOrder(
                            new Violation(Violation.Kind.NON_EXPOSED_TYPE, "order", BASE + ".order.OrderService",
                                    BASE + ".inventory.internal.Stock", HEADER + ORIGIN + "constructor <" + BASE
                                            + ".inventory.internal.Stock.<init>()> in (OrderService.java:9)"),
                            new Violation(Violation.Kind.NON_EXPOSED_TYPE, "order", BASE + ".order.OrderService",
                                    BASE + ".inventory.internal.Stock", HEADER + ORIGIN + "method <" + BASE
                                            + ".inventory.internal.Stock.available()> in (OrderService.java:9)")));

    @Test
    void failsTheBuild() {
    }
}
