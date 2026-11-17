package io.quarkiverse.modulith.deployment.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.Violation;
import io.quarkus.test.QuarkusExtensionTest;

public class RootsAndRulesConfigTest {

    static final String CONFIG = ExplicitlyAnnotatedConfigTest.CONFIG;
    static final String EXTRA = "io.quarkiverse.modulith.deployment.fixtures.model.configextra";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, CONFIG).addPackages(true, EXTRA)
                    .addClass(CountingRule.class))
            .overrideConfigKey("quarkus.modulith.base-package", CONFIG)
            .overrideConfigKey("quarkus.modulith.additional-packages", EXTRA)
            .overrideConfigKey("quarkus.modulith.ignored-packages", "..legacy")
            .overrideConfigKey("quarkus.modulith.shared-modules", "first")
            .overrideConfigKey("quarkus.modulith.verification.allow-field-injection", "false")
            .overrideConfigKey("quarkus.modulith.verification.additional-rules", CountingRule.class.getName())
            .overrideConfigKey("quarkus.modulith.verification.mode", "warn")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    ApplicationModules modules;

    @Test
    void readsRootsSharedModulesAndRules() {
        // act
        List<Violation> violations = modules.detectViolations().getViolations();

        // assert
        assertThat(modules.stream().map(ApplicationModule::getName)).containsExactly("extra", "first", "second");
        assertThat(modules.getSharedModules()).extracting(ApplicationModule::getName).containsExactly("first");
        assertThat(modules.getMetadata().getBasePackages()).containsExactly(CONFIG, EXTRA);
        assertThat(violations).containsExactly(
                new Violation(Violation.Kind.FIELD_INJECTION, "second", CONFIG + ".second.Second.first",
                        CONFIG + ".first.First", "Module Second uses field injection in " + CONFIG
                                + ".second.Second.first. Prefer constructor injection instead!"),
                new Violation(Violation.Kind.ADDITIONAL_RULE, "", "", "", "3 module(s)"));
    }
}
