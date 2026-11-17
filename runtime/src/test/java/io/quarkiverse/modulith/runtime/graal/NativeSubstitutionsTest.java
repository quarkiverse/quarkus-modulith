package io.quarkiverse.modulith.runtime.graal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ModelData;

class NativeSubstitutionsTest {

    @ParameterizedTest
    @CsvSource({ "java.lang.String,false", "com.acme.Missing,true" })
    void tellsWhetherAClassIsMissing(String name, boolean missing) {
        // act
        boolean result = MissingClass.isMissing(name, getClass().getClassLoader());

        // assert
        assertThat(result).isEqualTo(missing);
    }

    @ParameterizedTest
    @MethodSource("missingLibraries")
    void findsTheOptionalLibrariesMissingFromTheRuntime(BooleanSupplier missing) {
        // act
        boolean result = missing.getAsBoolean();

        // assert
        assertThat(result).isTrue();
    }

    static List<BooleanSupplier> missingLibraries() {
        return List.of(new ArchUnitMissing(), new JMoleculesRulesMissing());
    }

    @Test
    void hasNoJMoleculesRulesWithoutTheLibrary() {
        // act
        List<Object> rules = Target_RuleSupport_WithoutJMoleculesRules.jMoleculesRules();

        // assert
        assertThat(rules).isEmpty();
    }

    @Test
    void cannotEvaluateArchUnitRulesWithoutArchUnit() {
        // arrange
        ApplicationModules modules = new ApplicationModules(new ModelData("", "", List.of(), List.of(), ""));

        // act & assert
        assertThatThrownBy(() -> Target_RuleSupport_WithoutArchUnit.evaluate(List.of(), modules))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ArchUnit rules need com.tngtech.archunit:archunit, which is not part of the native"
                        + " executable");
    }
}
