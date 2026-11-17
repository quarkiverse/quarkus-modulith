package io.quarkiverse.modulith.core.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ClassLocations;
import io.quarkiverse.modulith.core.ModelData;
import io.quarkiverse.modulith.core.ModelData.ModuleData;

class ModelAnalyzerTest {

    static final String FIXTURES = "io.quarkiverse.modulith.core.fixtures";
    static final String SHOP = FIXTURES + ".shop";

    static AnalysisSettings settings(String basePackage, String strategy, ClassLoader classLoader) {
        return new AnalysisSettings(basePackage, Optional.empty(), Optional.empty(), List.of(), List.of(), Optional.empty(),
                strategy, List.of(), it -> false, true, classLoader, true);
    }

    static ClassPathFiles tests(ClassLoader classLoader) {
        return new ClassPathFiles(classLoader, ApplicationModulesLoader.filter(ClassLocations.TESTS));
    }

    /**
     * @param list comma-separated names, {@code null} for none
     */
    static String[] names(String list) {
        return Optional.ofNullable(list).map(it -> it.split(",")).orElseGet(() -> new String[0]);
    }

    static List<String> modules(AnalysisResult result) {
        return result.data().modules().stream().map(ModuleData::name).toList();
    }

    @Test
    void usesTheStrategyRegisteredAsServiceAndWarns(@TempDir Path directory) throws IOException {
        // arrange
        URLClassLoader classLoader = ServiceClassLoaders.with(directory,
                Map.of(ServiceClassLoaders.strategies(), RegisteredStrategy.class.getName()));

        // act
        AnalysisResult result;
        try (classLoader) {
            result = ModelAnalyzer.analyze(settings(SHOP, "", classLoader), tests(classLoader));
        }

        // assert
        assertThat(modules(result)).containsExactly("order");
        assertThat(result.warnings()).containsExactly("Registering the module detection strategy "
                + RegisteredStrategy.class.getName() + " in META-INF/services is deprecated! Set"
                + " quarkus.modulith.detection-strategy to its class name instead.");
    }

    @Test
    void rejectsTwoRegisteredStrategies(@TempDir Path directory) throws IOException {
        // arrange
        try (URLClassLoader classLoader = ServiceClassLoaders.with(directory, Map.of(ServiceClassLoaders.strategies(),
                RegisteredStrategy.class.getName() + "\n" + OtherStrategy.class.getName()))) {

            // act & assert
            assertThatThrownBy(() -> ModelAnalyzer.analyze(settings(SHOP, "", classLoader), tests(classLoader)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageStartingWith("Multiple module detection strategies configured. Only one supported! [");
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = { "direct-sub-packages;catalog,catalog.pricing,inventory,kernel,legacy,order",
            "explicitly-annotated;catalog.pricing,inventory,kernel,order",
            "io.quarkiverse.modulith.core.impl.RegisteredStrategy;order" })
    void usesTheConfiguredStrategy(String strategy, String names) {
        // arrange
        ClassLoader classLoader = getClass().getClassLoader();

        // act
        AnalysisResult result = ModelAnalyzer.analyze(settings(SHOP, strategy, classLoader), tests(classLoader));

        // assert
        assertThat(modules(result)).containsExactly(names(names));
        assertThat(result.warnings()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "com.acme.Missing", "java.lang.String" })
    void rejectsStrategiesThatCannotBeUsed(String strategy) {
        // arrange
        ClassLoader classLoader = getClass().getClassLoader();

        // act & assert
        assertThatThrownBy(() -> ModelAnalyzer.analyze(settings(SHOP, strategy, classLoader), tests(classLoader)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot use " + strategy + " as module detection strategy. Set"
                        + " quarkus.modulith.detection-strategy to direct-sub-packages, explicitly-annotated or the name"
                        + " of an ApplicationModuleDetectionStrategy with a public no-argument constructor.");
    }

    @Test
    void addsTheModulesOfSourceFactories(@TempDir Path directory) throws IOException {
        // arrange
        URLClassLoader classLoader = ServiceClassLoaders.with(directory,
                Map.of(ServiceClassLoaders.factories(), ContributingFactory.class.getName()));

        // act
        AnalysisResult result;
        try (classLoader) {
            result = ModelAnalyzer.analyze(settings(FIXTURES + ".scan", "", classLoader), tests(classLoader));
        }

        // assert
        assertThat(modules(result)).containsExactly("alpha", "two");
        assertThat(result.data().rootPackages()).containsExactly(FIXTURES + ".scan", FIXTURES + ".contrib");
    }

    @Test
    void rejectsDuplicateIdentifiers() {
        // arrange
        ClassLoader classLoader = getClass().getClassLoader();

        // act & assert
        assertThatThrownBy(() -> ModelAnalyzer.analyze(settings(FIXTURES + ".dup", "", classLoader), tests(classLoader)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Module identifier 'dup' is used by the packages [" + FIXTURES + ".dup.a, " + FIXTURES
                        + ".dup.b]. Give each module a unique identifier with @ApplicationModule(id = ...).\n"
                        + "Module identifier 'twice' is used by the packages [" + FIXTURES + ".dup.c, " + FIXTURES
                        + ".dup.d]. Give each module a unique identifier with @ApplicationModule(id = ...).");
    }

    @Test
    void appliesConfiguredSettings() {
        // arrange
        ClassLoader classLoader = getClass().getClassLoader();
        Predicate<String> ignored = it -> it.endsWith(".Old");
        AnalysisSettings settings = new AnalysisSettings(SHOP, Optional.of(SHOP + ".ShopApplication"),
                Optional.of("Configured"), List.of("legacy"), List.of(FIXTURES + ".listed"), Optional.of(true), "",
                List.of("..pricing..", SHOP + ".inventory"), ignored, true, classLoader, true);

        // act
        ModelData data = ModelAnalyzer.analyze(settings, tests(classLoader)).data();

        // assert
        assertThat(data.systemName()).isEqualTo("Configured");
        assertThat(data.sharedModules()).containsExactly("kernel", "legacy");
        assertThat(data.useFullyQualifiedModuleNames()).isTrue();
        assertThat(data.rootPackages()).containsExactly(SHOP, FIXTURES + ".listed");
        assertThat(data.modules()).extracting(ModuleData::name).containsExactly(FIXTURES + ".listed.alpha",
                SHOP + ".catalog", SHOP + ".order", "kernel");
    }

    @Test
    void ignoresSpringModulithAnnotationsWhenTurnedOff() {
        // arrange
        ClassLoader classLoader = getClass().getClassLoader();
        String explicit = "io.quarkiverse.modulith.core.par.explicit";
        AnalysisSettings settings = new AnalysisSettings(explicit, Optional.of(explicit + ".ExplicitApplication"),
                Optional.empty(), List.of(), List.of(), Optional.empty(), "", List.of(), it -> false, false, classLoader,
                true);

        // act
        ModelData data = ModelAnalyzer.analyze(settings, tests(classLoader)).data();

        // assert
        assertThat(data.systemName()).isEmpty();
        assertThat(data.modules()).allSatisfy(it -> assertThat(it.restricted()).isFalse());
        assertThat(data.modules()).filteredOn(it -> it.name().equals("store")).singleElement()
                .satisfies(it -> assertThat(it.namedInterfaces()).hasSize(1));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "io.quarkiverse.modulith.core.fixtures.shop;io.quarkiverse.modulith.core.fixtures.shop.ShopApplication;false;Shop;catalog,catalog.pricing,inventory,kernel,legacy,order",
            "io.quarkiverse.modulith.core.par.advanced;;false;;billing,customer,order",
            "io.quarkiverse.modulith.core.fixtures.blank;io.quarkiverse.modulith.core.fixtures.blank.BlankApplication;true;BlankApplication;" })
    void readsTheDeclaredApplication(String basePackage, String modulithType, boolean springAnnotations,
            String systemName, String names) {
        // arrange
        ClassLoader classLoader = getClass().getClassLoader();
        AnalysisSettings settings = new AnalysisSettings(basePackage, Optional.ofNullable(modulithType), Optional.empty(),
                List.of(), List.of(), Optional.empty(), "", List.of(), it -> false, springAnnotations, classLoader, true);

        // act
        ModelData data = ModelAnalyzer.analyze(settings, tests(classLoader)).data();

        // assert
        assertThat(data.systemName()).isEqualTo(Objects.requireNonNullElse(systemName, ""));
        assertThat(data.modules()).extracting(ModuleData::name).containsExactly(names(names));
    }

    @ParameterizedTest
    @CsvSource({ "io.quarkiverse.modulith.core.fixtures.shop,true,io.quarkiverse.modulith.core.fixtures.shop.ShopApplication",
            "io.quarkiverse.modulith.core.fixtures.shop,false,io.quarkiverse.modulith.core.fixtures.shop.ShopApplication",
            "io.quarkiverse.modulith.core.fixtures,true,",
            "io.quarkiverse.modulith.core.par.simple,false,",
            "io.quarkiverse.modulith.core.par.simple,true,io.quarkiverse.modulith.core.par.simple.SimpleApplication" })
    void findsTheModulithType(String basePackage, boolean springAnnotations, String expected) {
        // arrange
        ClassPathFiles files = tests(getClass().getClassLoader());

        // act
        Optional<String> type = ModelAnalyzer.findModulithType(files, basePackage, springAnnotations);

        // assert
        assertThat(type).isEqualTo(Optional.ofNullable(expected));
    }

    @Test
    void readsTheModulithTypeOfAPackage() {
        // act
        ApplicationModules fromPackage = ApplicationModules.of(SHOP, ClassLocations.TESTS);

        // assert
        assertThat(fromPackage.getSystemName()).isEqualTo("Shop");
        assertThat(fromPackage.getSource()).isEqualTo(SHOP + ".ShopApplication");
    }

    @Test
    void leavesTestClassesOutOfAPackageByDefault() {
        // act
        ApplicationModules production = ApplicationModules.of(SHOP);

        // assert
        assertThat(production.stream()).isEmpty();
    }

    @Test
    void usesThePackageAsSourceWithoutModulithType() {
        // act
        ApplicationModules scan = ApplicationModules.of(FIXTURES + ".scan", ClassLocations.ALL);

        // assert
        assertThat(scan.getSource()).isEqualTo(FIXTURES + ".scan");
    }
}
