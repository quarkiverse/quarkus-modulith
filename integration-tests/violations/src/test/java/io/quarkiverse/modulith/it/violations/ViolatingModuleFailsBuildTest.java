package io.quarkiverse.modulith.it.violations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.Violations;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Builds the application with the default verification mode. The build must fail because the billing module uses an
 * internal type of the customer module.
 */
public class ViolatingModuleFailsBuildTest {

    @RegisterExtension
    static final QuarkusExtensionTest build = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ViolationsApplication.class.getPackage()))
            .overrideConfigKey("quarkus.modulith.verification.mode", "fail")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .assertException(failure -> assertThat(violations(failure).orElseThrow().getViolations())
                    .containsExactlyInAnyOrderElementsOf(Expected.VIOLATIONS));

    static Optional<Violations> violations(Throwable failure) {
        return Stream.iterate(failure, Objects::nonNull, Throwable::getCause)
                .filter(Violations.class::isInstance).map(Violations.class::cast).findFirst();
    }

    @Test
    void buildFails() {
    }
}
