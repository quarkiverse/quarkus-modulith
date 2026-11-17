package io.quarkiverse.modulith.deployment.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.Violation;
import io.quarkus.test.QuarkusExtensionTest;

public class JMoleculesRulesConfigTest {

    static final String DDD = "io.quarkiverse.modulith.deployment.fixtures.model.ddd";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, DDD))
            .overrideConfigKey("quarkus.modulith.base-package", DDD)
            .overrideConfigKey("quarkus.modulith.verification.jmolecules-rules", "true")
            .overrideConfigKey("quarkus.modulith.verification.mode", "warn")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    ApplicationModules modules;

    @Test
    void runsTheJMoleculesRulesAtBuildTime() {
        // act
        var recorded = modules.getModelData().violations();

        // assert
        assertThat(recorded).extracting(it -> it.kind()).containsExactly(Violation.Kind.ADDITIONAL_RULE.name());
        assertThat(recorded).extracting(it -> it.message()).containsExactly("Field i.q.m.d.f.m.d.o.Order.customer refers to"
                + " an aggregate root (i.q.m.d.f.m.d.c.Customer). Rather use an identifier reference or Association!");
    }
}
