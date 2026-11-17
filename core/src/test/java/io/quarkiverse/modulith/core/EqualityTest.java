package io.quarkiverse.modulith.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.core.ModelData.ModuleData;
import io.quarkiverse.modulith.core.data.DependencyData;
import io.quarkiverse.modulith.core.fixtures.shop.ShopApplication;
import io.quarkiverse.modulith.core.fixtures.shop.catalog.internal.Stock;
import io.quarkiverse.modulith.core.par.jmolecules.JmoleculesApplication;
import io.quarkiverse.modulith.core.util.ApplicationModulesExporter;

class EqualityTest {

    static final ApplicationModules SHOP = ApplicationModules.of(ShopApplication.class, ClassLocations.TESTS);
    static final String INCLUDE_TEST_CLASSES = "quarkus.modulith.verification.include-test-classes";

    static ApplicationModule module(String name) {
        return SHOP.getModuleByName(name).orElseThrow();
    }

    static ApplicationModuleDependency dependency(int index) {
        return module("order").getDirectDependencies(SHOP).stream().skip(index).findFirst().orElseThrow();
    }

    @AfterEach
    void clearProperties() {
        System.clearProperty(INCLUDE_TEST_CLASSES);
    }

    @Test
    void comparesModulesByIdentifierAndBasePackage() {
        // arrange
        ApplicationModule order = module("order");

        // act
        ApplicationModule copy = Modules.module("order", order.getBasePackage());

        // assert
        assertThat(order).isEqualTo(copy).hasSameHashCodeAs(copy).isNotEqualTo(Modules.module("order", "other"));
    }

    @Test
    void tellsRootModulesFromModules() {
        // arrange
        ApplicationModule order = module("order");

        // act
        ApplicationModule root = new ApplicationModule(new ModuleData("order", "", order.getBasePackage(), false, false,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()), Map.of(), true);

        // assert
        assertThat(order).isNotEqualTo(root).isNotEqualTo(SHOP.getModuleForPackage(ShopModelTest.BASE).orElseThrow());
    }

    @Test
    void findsTheModuleOfAJavaType() {
        // arrange
        JavaType stock = Types.publicType(Stock.class.getName());

        // act
        boolean contained = SHOP.contains(stock);

        // assert
        assertThat(contained).isTrue();
        assertThat(SHOP.getModuleByType(stock).map(ApplicationModule::getName)).hasValue("catalog");
    }

    @ParameterizedTest
    @CsvSource({ "catalog.Unknown,true", "catalog.pricing.PriceList,false", "order.Unknown,false" })
    void decidesWhetherAModuleCouldContainAType(String type, boolean possible) {
        // act
        boolean result = module("catalog").couldContain(ShopModelTest.BASE + "." + type);

        // assert
        assertThat(result).isEqualTo(possible);
    }

    @Test
    void findsNoViolationsWithoutJMoleculesRules() {
        // act
        Violations violations = SHOP.detectViolations(VerificationOptions.defaults(false));

        // assert
        assertThat(violations.hasViolations()).isFalse();
    }

    @Test
    void declaresNoSystemNameWithoutModulithType() {
        // act
        ModulithMetadata metadata = ApplicationModules.of("io.quarkiverse.modulith.core.fixtures.scan",
                ClassLocations.ALL).getMetadata();

        // assert
        assertThat(metadata.getSystemName()).isEmpty();
    }

    @Test
    void recordsJMoleculesEventsWithoutSources() {
        // arrange
        ApplicationModules modules = ApplicationModules.of(JmoleculesApplication.class, ClassLocations.ALL);

        // act
        List<EventType> events = modules.getModuleByName("delivery").orElseThrow().getPublishedEventTypes();

        // assert
        assertThat(events).singleElement().satisfies(it -> assertThat(it.hasSources()).isFalse());
    }

    @Test
    void describesDependencies() {
        // act
        String described = dependency(0).toString();

        // assert
        assertThat(described).isEqualTo("Component i.q.m.c.f.s.o.OrderManagement using i.q.m.c.f.s.c.a.Products");
    }

    @Test
    void comparesDependenciesByDataAndTarget() {
        // act
        ApplicationModuleDependency again = dependency(0);

        // assert
        assertThat(dependency(0)).isEqualTo(again).hasSameHashCodeAs(again).isNotEqualTo(dependency(1))
                .isNotEqualTo("dependency");
        assertThat(new DefaultApplicationModuleDependency(new DependencyData("a", "b", "DEFAULT", "", ""), null, null,
                module("kernel"))).isNotEqualTo(new DefaultApplicationModuleDependency(
                        new DependencyData("a", "b", "DEFAULT", "", ""), null, null, module("catalog")));
    }

    @Test
    void rendersEmptyDependencies() {
        // act
        ApplicationModuleDependencies entities = module("order").getDirectDependencies(SHOP)
                .withType(DependencyType.ENTITY);

        // assert
        assertThat(entities).hasToString("[]");
    }

    @Test
    void comparesBeansByTypeAndModule() {
        // arrange
        ModuleBean products = module("catalog").getBeans().get(0);

        // act
        boolean equalToOtherModule = products.equals(new ModuleBean(products.getType(), module("order")));

        // assert
        assertThat(equalToOtherModule).isFalse();
        assertThat(products).isNotEqualTo(module("catalog").getBeans().get(1)).isNotEqualTo("bean")
                .isEqualTo(new ModuleBean(products.getType(), module("catalog")));
    }

    @Test
    void comparesAllowedDependenciesByTarget() {
        // act
        AllowedDependencies dependencies = module("order").getAllowedDependencies(SHOP);

        // assert
        assertThat(dependencies).extracting(AllowedDependency::toString).containsExactly("kernel", "catalog :: api");
        List<AllowedDependency> allowed = dependencies.stream().toList();
        assertThat(allowed.get(0)).isEqualTo(AllowedDependency.to(module("kernel")))
                .hasSameHashCodeAs(AllowedDependency.to(module("kernel"))).isNotEqualTo(allowed.get(1))
                .isNotEqualTo(AllowedDependency.to(module("order"))).isNotEqualTo("kernel");
    }

    @ParameterizedTest
    @CsvSource({ "catalog::*,catalog::**", "catalog,catalog::api" })
    void tellsWildcardsAndInterfacesApart(String left, String right) {
        // act
        AllowedDependency dependency = AllowedDependency.of(left, module("order"), SHOP);

        // assert
        assertThat(dependency).isNotEqualTo(AllowedDependency.of(right, module("order"), SHOP));
    }

    @Test
    void rendersWildcardDependencies() {
        // act
        AllowedDependency all = AllowedDependency.of("catalog::*", module("order"), SHOP);

        // assert
        assertThat(all).hasToString("catalog :: *");
    }

    @Test
    void grantsEverythingAModuleExposes() {
        // act
        AllowedDependency everything = AllowedDependency.of("catalog::**", module("order"), SHOP);

        // assert
        assertThat(everything.contains(Types.publicType(ShopModelTest.BASE + ".catalog.Catalog"))).isTrue();
    }

    @Test
    void comparesAllowedDependenciesOfModules() {
        // act
        AllowedDependencies order = module("order").getAllowedDependencies(SHOP);

        // assert
        assertThat(order).isNotEqualTo(module("inventory").getAllowedDependencies(SHOP));
    }

    @Test
    void contributesNoModulesByDefault() {
        // act
        List<ApplicationModuleSource> sources = new RootOnlyFactory().getApplicationModuleSources(it -> null, false)
                .toList();

        // assert
        assertThat(sources).isEmpty();
    }

    @Test
    void printsTheExportToStandardOutput() throws Exception {
        // arrange
        System.setProperty(INCLUDE_TEST_CLASSES, "true");
        String expected = new ApplicationModulesExporter(ApplicationModules.of(ShopModelTest.BASE)).toFullJson();
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        // act
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        try {
            ApplicationModulesExporter.main(new String[] { ShopModelTest.BASE });
        } finally {
            System.setOut(original);
        }

        // assert
        assertThat(output.toString(StandardCharsets.UTF_8)).isEqualTo(expected + System.lineSeparator())
                .startsWith("{ \"catalog\" : {");
    }
}
