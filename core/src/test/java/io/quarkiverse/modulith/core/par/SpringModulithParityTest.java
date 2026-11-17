package io.quarkiverse.modulith.core.par;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkiverse.modulith.core.par.advanced.AdvancedApplication;
import io.quarkiverse.modulith.core.par.annotated.AnnotatedApplication;
import io.quarkiverse.modulith.core.par.beans.BeansApplication;
import io.quarkiverse.modulith.core.par.custom.CustomApplication;
import io.quarkiverse.modulith.core.par.cycle.CycleApplication;
import io.quarkiverse.modulith.core.par.explicit.ExplicitApplication;
import io.quarkiverse.modulith.core.par.fqn.FqnApplication;
import io.quarkiverse.modulith.core.par.interfaces.InterfacesApplication;
import io.quarkiverse.modulith.core.par.jmolecules.JmoleculesApplication;
import io.quarkiverse.modulith.core.par.nested.NestedApplication;
import io.quarkiverse.modulith.core.par.open.OpenApplication;
import io.quarkiverse.modulith.core.par.propagate.PropagateApplication;
import io.quarkiverse.modulith.core.par.samename.SameNameApplication;
import io.quarkiverse.modulith.core.par.shapes.ShapesApplication;
import io.quarkiverse.modulith.core.par.shared.SharedApplication;
import io.quarkiverse.modulith.core.par.simple.SimpleApplication;
import io.quarkiverse.modulith.core.par.strategy.QuarkusModulesStrategy;
import io.quarkiverse.modulith.core.par.strategy.SpringModulesStrategy;

/**
 * Builds the model of each fixture with Spring Modulith 2.1.2 and with Quarkus Modulith and compares modules, named
 * interfaces, dependencies with their types, violations, events, aggregate roots, value types and beans.
 */
class SpringModulithParityTest {

    @AfterEach
    void clearProperties() {
        System.clearProperty("spring.modulith.detection-strategy");
        System.clearProperty("quarkus.modulith.detection-strategy");
        System.clearProperty("quarkus.modulith.verification.allow-field-injection");
    }

    static Stream<Arguments> fixtures() {
        return Stream.of(
                Arguments.of(SimpleApplication.class, "", ""),
                Arguments.of(AdvancedApplication.class, "", ""),
                Arguments.of(NestedApplication.class, "", ""),
                Arguments.of(OpenApplication.class, "", ""),
                Arguments.of(SharedApplication.class, "", ""),
                Arguments.of(ExplicitApplication.class, "", ""),
                Arguments.of(InterfacesApplication.class, "", ""),
                Arguments.of(PropagateApplication.class, "", ""),
                Arguments.of(AnnotatedApplication.class, "explicitly-annotated", "explicitly-annotated"),
                Arguments.of(CustomApplication.class, SpringModulesStrategy.class.getName(),
                        QuarkusModulesStrategy.class.getName()),
                Arguments.of(JmoleculesApplication.class, "", ""),
                Arguments.of(CycleApplication.class, "", ""),
                Arguments.of(FqnApplication.class, "", ""),
                Arguments.of(BeansApplication.class, "", ""),
                Arguments.of(ShapesApplication.class, "", ""),
                Arguments.of(SameNameApplication.class, "", ""));
    }

    @ParameterizedTest
    @MethodSource("fixtures")
    void quarkusModulithBuildsTheModelSpringModulithBuilds(Class<?> application, String springStrategy,
            String quarkusStrategy) {
        // arrange
        System.setProperty("spring.modulith.detection-strategy", springStrategy);
        System.setProperty("quarkus.modulith.detection-strategy", quarkusStrategy);
        System.setProperty("quarkus.modulith.verification.allow-field-injection", "false");

        // act
        ModelSnapshot spring = SpringSnapshots.of(application);
        ModelSnapshot quarkus = QuarkusSnapshots.of(application);

        // assert
        assertThat(quarkus.modules()).isEqualTo(spring.modules()).isNotEmpty();
        assertThat(quarkus.namedInterfaces()).isEqualTo(spring.namedInterfaces());
        assertThat(quarkus.dependencies()).isEqualTo(spring.dependencies());
        assertThat(quarkus.violations()).isEqualTo(spring.violations());
        assertThat(quarkus.details()).isEqualTo(spring.details());
    }

    @Test
    void countsThrowsClausesAndTypeBoundsButNotCastsArraysCatchesTypeUsesOrLocals() {
        // arrange
        System.setProperty("quarkus.modulith.verification.allow-field-injection", "false");
        String shapes = "io.quarkiverse.modulith.core.par.shapes.";

        // act
        ModelSnapshot spring = SpringSnapshots.of(ShapesApplication.class);
        ModelSnapshot quarkus = QuarkusSnapshots.of(ShapesApplication.class);

        // assert
        assertThat(spring.dependencies()).containsExactly(
                "bound: " + shapes + "bound.Bounded -> " + shapes + "target.internal.Thing (DEFAULT)",
                "throwing: " + shapes + "throwing.Thrower -> " + shapes + "target.internal.Problem (DEFAULT)");
        assertThat(quarkus.dependencies()).isEqualTo(spring.dependencies());
    }
}
