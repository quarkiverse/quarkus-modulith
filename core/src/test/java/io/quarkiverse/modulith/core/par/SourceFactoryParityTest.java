package io.quarkiverse.modulith.core.par;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.core.par.factory.FactoryApplication;

/**
 * Runs in a Surefire execution of its own, whose class path registers a source factory for Spring Modulith
 * ({@code spring.factories}) and for Quarkus Modulith ({@code META-INF/services}). Both apply to every model of the
 * JVM, so the other parity tests must not see them.
 */
class SourceFactoryParityTest {

    @Test
    void contributedModulesMatchSpringModulith() {
        // act
        ModelSnapshot spring = SpringSnapshots.of(FactoryApplication.class);
        ModelSnapshot quarkus = QuarkusSnapshots.of(FactoryApplication.class);

        // assert
        assertThat(quarkus).isEqualTo(spring);
        assertThat(quarkus.modules()).hasSize(3);
    }
}
