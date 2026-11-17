package io.quarkiverse.modulith.it.moduletests;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.Violations;
import io.quarkiverse.modulith.it.fixtures.broken.BrokenApplication;
import io.quarkiverse.modulith.test.TestApplicationModules;
import io.quarkus.test.junit.QuarkusTest;

/**
 * A {@code @Modulith} type in the test sources, here the fixture application of {@code TestApplicationModules.of},
 * neither breaks the build of the application nor replaces its {@code @Modulith} type.
 */
@QuarkusTest
class TestSourcesModulithTest {

    @Inject
    ApplicationModules modules;

    @Test
    void buildsTheModelOfTheApplicationClasses() {
        // act
        Object source = modules.getSource();

        // assert
        assertThat(source).isEqualTo(ModuleTestsApplication.class.getName());
        assertThat(modules.getSystemName()).isEqualTo("Module tests");
    }

    @Test
    void verifiesTheFixtureApplicationOfTheTestSources() {
        // arrange
        ApplicationModules fixture = TestApplicationModules.of(BrokenApplication.class);

        // act
        Violations violations = fixture.detectViolations();

        // assert
        assertThat(fixture.getSystemName()).isEqualTo("Broken");
        assertThat(violations.getMessage()).contains("Module 'invoices' depends on non-exposed type "
                + "io.quarkiverse.modulith.it.fixtures.broken.orders.internal.Ledger within module 'orders'!");
    }
}
