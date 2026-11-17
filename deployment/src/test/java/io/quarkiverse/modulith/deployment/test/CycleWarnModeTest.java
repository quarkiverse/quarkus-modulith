package io.quarkiverse.modulith.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.Violation;
import io.quarkiverse.modulith.core.Violations;
import io.quarkus.test.QuarkusExtensionTest;

public class CycleWarnModeTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, CycleFailsBuildTest.BASE))
            .overrideConfigKey("quarkus.modulith.base-package", CycleFailsBuildTest.BASE)
            .overrideConfigKey("quarkus.modulith.verification.mode", "warn")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .setLogRecordPredicate(it -> it.getLoggerName().startsWith("io.quarkiverse.modulith"))
            .assertLogRecords(records -> assertThat(records).extracting(it -> it.getMessage())
                    .contains("- " + CycleFailsBuildTest.MESSAGE));

    @Inject
    ApplicationModules modules;

    @Test
    void startsAndReportsTheRecordedViolations() {
        // act
        Violations violations = modules.detectViolations();

        // assert
        assertThat(violations.getViolations()).containsExactly(
                new Violation(Violation.Kind.CYCLE, "a", "a -> b -> a", "", CycleFailsBuildTest.MESSAGE));
        assertThatThrownBy(modules::verify).isSameAs(violations);
    }
}
