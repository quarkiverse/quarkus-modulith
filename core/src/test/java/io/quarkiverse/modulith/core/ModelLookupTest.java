package io.quarkiverse.modulith.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.core.ModelData.ModuleData;
import io.quarkiverse.modulith.core.fixtures.shop.ShopApplication;

class ModelLookupTest {

    private static final ApplicationModules SHOP = ApplicationModules.of(ShopApplication.class, ClassLocations.TESTS);

    private static ModuleData module(String name, List<String> allowedDependencies) {
        return new ModuleData(name, name, "com.acme." + name, false, !allowedDependencies.isEmpty(),
                allowedDependencies, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static ApplicationModules declaring(String declaration) {
        return new ApplicationModules(new ModelData("", "com.acme",
                List.of(module("client", List.of(declaration)), module("store", List.of())), List.of(), ""));
    }

    @ParameterizedTest
    @ValueSource(strings = { "store::api::x", "store :: api :: x", "store::::x" })
    void rejectsAllowedDependenciesWithMoreThanTwoSegments(String declaration) {
        // arrange
        ApplicationModules modules = declaring(declaration);
        ApplicationModule client = modules.getModuleByName("client").orElseThrow();

        // act & assert
        assertThatThrownBy(() -> client.getAllowedDependencies(modules)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid allowed dependency '" + declaration
                        + "' in client! Use module, module::name, module::* or module::**.");
    }

    @Test
    void reportsAllowedDependenciesWithMoreThanTwoSegments() {
        // arrange
        ApplicationModules modules = declaring("store::api::x");

        // act
        List<Violation> violations = ModuleVerifier.invalidDeclarations(modules);

        // assert
        assertThat(violations).containsExactly(new Violation(Violation.Kind.INVALID_DECLARATION, "client",
                "com.acme.client", "store::api::x",
                "Invalid allowed dependency 'store::api::x' in client! Use module, module::name, module::* or module::**."));
    }

    @Test
    void resolvesDependenciesAgainstTheGivenModel() {
        // arrange
        ApplicationModules other = new ApplicationModules(SHOP.getModelData());
        ApplicationModule order = SHOP.getModuleByName("order").orElseThrow();
        order.getDirectDependencies(SHOP);

        // act
        Set<ApplicationModule> targets = order.getDirectDependencies(other).uniqueModules();

        // assert
        assertThat(targets).hasSize(2).allSatisfy(it -> assertThat(it)
                .isSameAs(other.getModuleByName(it.getName()).orElseThrow()));
    }

    @Test
    void resolvesDependenciesAgainAfterAnotherModel() {
        // arrange
        ApplicationModules other = new ApplicationModules(SHOP.getModelData());
        ApplicationModule order = SHOP.getModuleByName("order").orElseThrow();
        order.getDirectDependencies(other);

        // act
        Set<ApplicationModule> targets = order.getDirectDependencies(SHOP).uniqueModules();

        // assert
        assertThat(targets).hasSize(2).allSatisfy(it -> assertThat(it)
                .isSameAs(SHOP.getModuleByName(it.getName()).orElseThrow()));
    }

    @Test
    void ordersModulesOfOtherModelsFirst() {
        // arrange
        ApplicationModule foreign = Modules.module("foreign", "com.foreign");

        // act
        int result = SHOP.getComparator().compare(foreign, SHOP.getModuleByName("catalog").orElseThrow());

        // assert
        assertThat(result).isNegative();
    }

    @Test
    void keepsTheParentOfAModule() {
        // arrange
        ApplicationModule pricing = SHOP.getModuleByName("catalog.pricing").orElseThrow();

        // act
        ApplicationModule parent = SHOP.getParentOf(pricing).orElseThrow();

        // assert
        assertThat(parent).isSameAs(SHOP.getParentOf(pricing).orElseThrow())
                .isEqualTo(SHOP.getModuleByName("catalog").orElseThrow());
    }
}
