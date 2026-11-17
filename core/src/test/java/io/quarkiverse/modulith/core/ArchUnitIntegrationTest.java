package io.quarkiverse.modulith.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import io.quarkiverse.modulith.core.fixtures.shop.ShopApplication;

class ArchUnitIntegrationTest {

    static final String SHOP = "io.quarkiverse.modulith.core.fixtures.shop";
    static final String INCLUDE_TEST_CLASSES = "quarkus.modulith.verification.include-test-classes";
    static final String STOCK_FIELD = "Field <" + SHOP + ".catalog.pricing.PriceList.stock> has type <" + SHOP
            + ".catalog.internal.Stock> in (PriceList.java:0)";
    static final ArchRule NO_INTERNALS = ArchRuleDefinition.noClasses().that().resideInAPackage("..pricing..")
            .should().dependOnClassesThat().resideInAPackage("..internal..");
    static final ApplicationModules MODULES = ApplicationModules.of(ShopApplication.class, ClassLocations.TESTS);

    @AfterEach
    void clearProperties() {
        System.clearProperty(INCLUDE_TEST_CLASSES);
    }

    @Test
    void leavesOutTheClassesOfAPredicate() {
        // arrange
        System.setProperty(INCLUDE_TEST_CLASSES, "true");

        // act
        ApplicationModules modules = ApplicationModules.of(ShopApplication.class,
                Filters.withoutModules("legacy", "pricing").or(JavaClass.Predicates.resideInAPackage(SHOP + ".kernel")));

        // assert
        assertThat(modules.stream().map(ApplicationModule::getName)).containsExactlyInAnyOrder("catalog", "order",
                "inventory");
    }

    @Test
    void leavesOutTheClassesOfAModuleOfAPackage() {
        // arrange
        System.setProperty(INCLUDE_TEST_CLASSES, "true");

        // act
        ApplicationModules modules = ApplicationModules.of(SHOP, Filters.withoutModule("legacy"));

        // assert
        assertThat(modules.stream().map(ApplicationModule::getName)).containsExactlyInAnyOrder("catalog", "kernel",
                "catalog.pricing", "order", "inventory");
    }

    @Test
    void readsTheClassFilesAnImportOptionAccepts() {
        // act
        ApplicationModules modules = ApplicationModules.of(ShopApplication.class, ImportOption.Predefined.ONLY_INCLUDE_TESTS);

        // assert
        assertThat(modules.stream().map(ApplicationModule::getName)).containsExactly("catalog", "kernel", "legacy",
                "catalog.pricing", "order", "inventory");
    }

    @Test
    void readsTheClassFilesOfAPackageAnImportOptionAccepts() {
        // act
        ApplicationModules modules = ApplicationModules.of(SHOP, ImportOption.Predefined.DO_NOT_INCLUDE_TESTS);

        // assert
        assertThat(modules.stream()).isEmpty();
    }

    @Test
    void runsAdditionalArchUnitRulesAndModuleRules() {
        // arrange
        ModulithRule rule = it -> new Violations(List.of(new Violation(Violation.Kind.ADDITIONAL_RULE, "", "", "", "own")));

        // act
        Violations violations = MODULES.detectViolations(VerificationOptions.defaults().withAdditionalVerifications(
                NO_INTERNALS).andRules(rule));

        // assert
        assertThat(violations.getMessages()).containsExactlyInAnyOrder("own", STOCK_FIELD);
    }

    @Test
    void addsArchUnitRulesToTheCurrentOnes() {
        // arrange
        VerificationOptions options = VerificationOptions.defaults().withoutAdditionalVerifications()
                .andAdditionalVerifications(List.of(NO_INTERNALS)).andAdditionalVerifications(NO_INTERNALS);

        // act
        Violations violations = MODULES.detectViolations(options);

        // assert
        assertThat(violations.getMessages()).containsExactly(STOCK_FIELD, STOCK_FIELD);
    }

    @Test
    void replacesTheArchUnitRules() {
        // act
        VerificationOptions options = VerificationOptions.defaults().withAdditionalVerifications(List.of(NO_INTERNALS));

        // assert
        assertThat(options.getAdditionalVerifications()).containsExactly(NO_INTERNALS);
    }

    @ParameterizedTest
    @CsvSource({ "false,0", "true,5" })
    void runsTheJMoleculesRulesByDefaultWhenPresent(boolean present, int rules) {
        // act
        VerificationOptions options = VerificationOptions.defaults(present);

        // assert
        assertThat(options.getAdditionalVerifications()).hasSize(rules);
    }

    @Test
    void addsTheJMoleculesRules() {
        // act
        VerificationOptions options = VerificationOptions.defaults(false).andJMoleculesRules();

        // assert
        assertThat(options.getAdditionalVerifications()).hasSize(5);
    }

    @Test
    void explainsThatTheJMoleculesRulesNeedTheirLibrary() {
        // arrange
        VerificationOptions options = VerificationOptions.defaults(false);

        // act & assert
        assertThatThrownBy(() -> options.andJMoleculesRules(false))
                .isInstanceOf(IllegalStateException.class).hasMessage("The jMolecules rules need"
                        + " org.jmolecules.integrations:jmolecules-archunit on the class path. Add it, or turn"
                        + " quarkus.modulith.verification.jmolecules-rules off.");
    }
}
