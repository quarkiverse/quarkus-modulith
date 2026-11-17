package io.quarkiverse.modulith.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.Violation;
import io.quarkiverse.modulith.deployment.Failures;
import io.quarkus.test.QuarkusExtensionTest;

public class CycleFailsBuildTest {

    static final String BASE = "io.quarkiverse.modulith.deployment.fixtures.cycle";
    static final String MESSAGE = "Cycle detected: a -> b -> a\n  1. Dependencies of a\n    - Field <" + BASE
            + ".a.A.b> has type <" + BASE + ".b.B> in (A.java:0)\n  2. Dependencies of b\n    - Field <" + BASE
            + ".b.B.a> has type <" + BASE + ".a.A> in (B.java:0)";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, BASE))
            .overrideConfigKey("quarkus.modulith.base-package", BASE)
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .assertException(t -> assertThat(Failures.violations(t).orElseThrow(() -> new AssertionError(t))
                    .getViolations())
                    .containsExactly(new Violation(Violation.Kind.CYCLE, "a", "a -> b -> a", "", MESSAGE)));

    @Test
    void failsTheBuild() {
    }
}
