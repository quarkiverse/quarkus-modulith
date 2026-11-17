package io.quarkiverse.modulith.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ModelData;
import io.quarkiverse.modulith.core.ModelData.ModuleData;
import io.quarkiverse.modulith.junit.diff.ModifiedFile;
import io.quarkiverse.modulith.junit.fixtures.app.AppFixture;
import io.quarkiverse.modulith.junit.fixtures.app.catalog.CatalogFixture;
import io.quarkiverse.modulith.junit.fixtures.app.inventory.InventoryFixture;
import io.quarkiverse.modulith.junit.fixtures.app.order.OrderFixture;
import io.quarkiverse.modulith.junit.fixtures.multiple.FirstApplication;
import io.quarkiverse.modulith.junit.fixtures.plain.PlainFixture;

class TestExecutionConditionTests {

    private static final String APP = "io.quarkiverse.modulith.junit.fixtures.app";

    private static final String APP_SOURCES = "src/main/java/io/quarkiverse/modulith/junit/fixtures/app/";

    private final List<String> requestedBasePackages = new ArrayList<>();

    @Test
    void fallsBackToEnabledTestIfMultipleApplicationTypesFound() {
        // arrange
        var context = new ConditionContext(FirstApplication.class, changes("Foo.java"), Optional.empty());

        // act
        var result = condition(shopModules()).evaluate(context);

        // assert
        assertEnabled(result, "Unable to locate the application base package of "
                + "io.quarkiverse.modulith.junit.fixtures.multiple.FirstApplication. Annotate exactly one type with "
                + "@Modulith or set quarkus.modulith.base-package, executing test.");
        assertThat(requestedBasePackages).isEmpty();
    }

    @Test
    void fallsBackToEnabledTestWithoutApplicationType() {
        // arrange
        var context = new ConditionContext(PlainFixture.class, changes("Foo.java"), Optional.empty());

        // act
        var result = condition(shopModules()).evaluate(context);

        // assert
        assertEnabled(result, "Unable to locate the application base package of "
                + "io.quarkiverse.modulith.junit.fixtures.plain.PlainFixture. Annotate exactly one type with "
                + "@Modulith or set quarkus.modulith.base-package, executing test.");
    }

    @Test
    void disablesForNoClassChangesWithPropertyConfiguration() {
        // arrange
        var context = new ConditionContext(OrderFixture.class, changes(OnNoChange.SKIP_ALL, "README.md"),
                Optional.empty());

        // act
        var result = condition(shopModules()).evaluate(context);

        // assert
        assertDisabled(result,
                "No source file changes detected, tests skipped due to configuration \"on-no-changes=skip-all\".");
    }

    @Test
    void enablesForNoClassChangesByDefault() {
        // arrange
        var context = new ConditionContext(OrderFixture.class, changes("README.md"), Optional.empty());

        // act
        var result = condition(shopModules()).evaluate(context);

        // assert
        assertEnabled(result, "No source file changes detected, running full test suite due to default configuration.");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "src/main/resources/application.properties | Some classpath resource changed, running all tests.",
            "pom.xml | Build resource changed, running all tests."
    })
    void runsAllTestsForResourceChanges(String file, String message) {
        // arrange
        var context = new ConditionContext(CatalogFixture.class,
                changes(OnNoChange.SKIP_ALL, file, APP_SOURCES + "order/Order.java"), Optional.empty());

        // act
        var result = condition(shopModules()).evaluate(context);

        // assert
        assertEnabled(result, message);
        assertThat(requestedBasePackages).isEmpty();
    }

    @Test
    void runsChangedTestClass() {
        // arrange
        var context = new ConditionContext(CatalogFixture.class, changes(OnNoChange.SKIP_ALL,
                "src/test/java/io/quarkiverse/modulith/junit/fixtures/app/catalog/CatalogFixture.java",
                APP_SOURCES + "order/Order.java"), Optional.empty());

        // act
        var result = condition(shopModules()).evaluate(context);

        // assert
        assertEnabled(result, "Change in test class detected, executing test.");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "order/Order.java | Changes detected in module order, executing test.",
            "order/internal/OrderRepository.java | Changes detected in module order, executing test.",
            "inventory/Stock.java | Changes detected in dependent module inventory, executing test.",
            "catalog/Product.java | Changes detected in dependent module catalog, executing test."
    })
    void runsTestsOfChangedAndDependentModules(String file, String message) {
        // arrange
        var context = new ConditionContext(OrderFixture.class, changes(APP_SOURCES + file), Optional.empty());

        // act
        var result = condition(shopModules()).evaluate(context);

        // assert
        assertEnabled(result, message);
        assertThat(requestedBasePackages).containsExactly(APP);
    }

    @ParameterizedTest
    @CsvSource({
            "order/Order.java",
            "shipping/Parcel.java",
            "Shop.java"
    })
    void skipsTestsOfModulesNotAffected(String file) {
        // arrange
        var context = new ConditionContext(InventoryFixture.class,
                changes(APP_SOURCES + file, "src/main/java/com/other/Unrelated.java"), Optional.empty());

        // act
        var result = condition(shopModules()).evaluate(context);

        // assert
        assertDisabled(result, "Test residing in module inventory not affected by changes!");
    }

    @Test
    void runsTestsOutsideOfModules() {
        // arrange
        var context = new ConditionContext(PlainFixture.class, changes(APP_SOURCES + "order/Order.java"),
                Optional.of(APP));

        // act
        var result = condition(shopModules()).evaluate(context);

        // assert
        assertEnabled(result,
                "Test in package io.quarkiverse.modulith.junit.fixtures.plain does not reside in any module!");
    }

    @Test
    void alwaysRunsTestsOfRootModule() {
        // arrange
        var modules = modules(module("shop", APP), module("order", APP + ".order"));
        var context = new ConditionContext(AppFixture.class, changes(APP_SOURCES + "order/Order.java"),
                Optional.empty());

        // act
        var result = condition(modules).evaluate(context);

        // assert
        assertEnabled(result, "Always executing tests in root modules.");
    }

    @Test
    void usesConfiguredBasePackage() {
        // arrange
        var context = new ConditionContext(PlainFixture.class, changes(APP_SOURCES + "order/Order.java"),
                Optional.of("io.quarkiverse.modulith.junit.fixtures"));
        var modules = modules(module("plain", "io.quarkiverse.modulith.junit.fixtures.plain"),
                module("app", APP));

        // act
        var result = condition(modules).evaluate(context);

        // assert
        assertDisabled(result, "Test residing in module plain not affected by changes!");
        assertThat(requestedBasePackages).containsExactly("io.quarkiverse.modulith.junit.fixtures");
    }

    @Test
    void runsTestIfModuleModelIsUnavailable() {
        // arrange
        var condition = new TestExecutionCondition(basePackage -> {
            requestedBasePackages.add(basePackage);
            throw new UnsupportedOperationException("Not implemented yet");
        }, new BasePackageFinder());
        var context = new ConditionContext(OrderFixture.class, changes(APP_SOURCES + "order/Order.java"),
                Optional.empty());

        // act
        var result = condition.evaluate(context);

        // assert
        assertEnabled(result, "Application module model of base package " + APP
                + " unavailable (Not implemented yet), executing test.");
        assertThat(requestedBasePackages).containsExactly(APP);
    }

    @Test
    void remembersThatTheModuleModelIsUnavailable() {
        // arrange
        var condition = new TestExecutionCondition(basePackage -> {
            requestedBasePackages.add(basePackage);
            throw new UnsupportedOperationException("Not implemented yet");
        }, new BasePackageFinder());
        var changes = changes(APP_SOURCES + "order/Order.java");
        condition.evaluate(new ConditionContext(OrderFixture.class, changes, Optional.empty()));

        // act
        var result = condition.evaluate(new ConditionContext(InventoryFixture.class, changes, Optional.empty()));

        // assert
        assertEnabled(result, "Application module model of base package " + APP
                + " unavailable (Not implemented yet), executing test.");
        assertThat(requestedBasePackages).containsExactly(APP);
    }

    @Test
    void buildsModuleModelOncePerBasePackage() {
        // arrange
        var condition = condition(shopModules());
        var changes = changes(APP_SOURCES + "catalog/Product.java");
        condition.evaluate(new ConditionContext(OrderFixture.class, changes, Optional.empty()));

        // act
        var result = condition.evaluate(new ConditionContext(InventoryFixture.class, changes, Optional.empty()));

        // assert
        assertEnabled(result, "Changes detected in dependent module catalog, executing test.");
        assertThat(requestedBasePackages).containsExactly(APP);
    }

    @Test
    void computesTransitiveDependenciesAcrossCyclesAndUnknownModules() {
        // arrange
        var a = module("a", "com.acme.a", "b", "ghost");
        var b = module("b", "com.acme.b", "a", "c");
        var c = module("c", "com.acme.c", "b");
        var modules = new ApplicationModules(new ModelData("system", "com.acme", List.of(a, b, c), List.of(), ""));

        // act
        var result = TestExecutionCondition.allDependencies(modules.getModuleByName("a").orElseThrow(), modules);

        // assert
        assertThat(result).containsExactly("b", "ghost", "a", "c");
    }

    private TestExecutionCondition condition(ApplicationModules modules) {
        Function<String, ApplicationModules> factory = basePackage -> {
            requestedBasePackages.add(basePackage);
            return modules;
        };
        return new TestExecutionCondition(factory, new BasePackageFinder());
    }

    private static ApplicationModules shopModules() {
        return modules(module("order", APP + ".order", "inventory"),
                module("inventory", APP + ".inventory", "catalog"),
                module("catalog", APP + ".catalog"),
                module("shipping", APP + ".shipping", "order"));
    }

    private static ApplicationModules modules(ModuleData... modules) {
        return new ApplicationModules(new ModelData("Shop", APP, List.of(modules), List.of(), ""));
    }

    private static ModuleData module(String name, String basePackage, String... dependencies) {
        return new ModuleData(name, name, basePackage, false, false, List.of(), List.of(), List.of(dependencies),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of());
    }

    private static Changes changes(String... files) {
        return changes(OnNoChange.EXECUTE_ALL, files);
    }

    private static Changes changes(OnNoChange onNoChange, String... files) {
        return Changes.of(ModifiedFile.of(files), onNoChange);
    }

    private static void assertEnabled(ConditionEvaluationResult result, String message) {
        assertThat(result.isDisabled()).isFalse();
        assertThat(result.getReason()).contains(message);
    }

    private static void assertDisabled(ConditionEvaluationResult result, String message) {
        assertThat(result.isDisabled()).isTrue();
        assertThat(result.getReason()).contains(message);
    }
}
