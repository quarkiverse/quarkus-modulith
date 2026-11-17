package io.quarkiverse.modulith.deployment.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.deployment.moduletest.SetupResource;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The {@code ddd} fixture breaks a jMolecules rule only. With the rules off, a module test that verifies
 * automatically starts.
 */
public class JMoleculesRulesDisabledTest {

    static final String DDD = "io.quarkiverse.modulith.deployment.fixtures.model.ddd";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, DDD).addClass(SetupResource.class))
            .overrideConfigKey("quarkus.modulith.base-package", DDD)
            .overrideConfigKey("quarkus.modulith.verification.jmolecules-rules", "false")
            .overrideConfigKey("quarkus.modulith.test.module", "orders")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    ApplicationModules modules;

    @Test
    void startsTheModuleTestWithoutTheDisabledRules() {
        // act
        String setup = SetupResource.read();

        // assert
        assertThat(setup).startsWith("module=orders\n");
        assertThat(modules.getModelData().violations()).isEmpty();
    }
}
