package io.quarkiverse.modulith.deployment.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeploymentModelTest {

    static final String VALID = "io.quarkiverse.modulith.deployment.fixtures.valid";
    static final String EDGE = "io.quarkiverse.modulith.deployment.fixtures.model.edge";
    static final String TWICE = "io.quarkiverse.modulith.deployment.fixtures.model.twice";
    static final ModuleModel MODEL = Fixtures.model(VALID, Optional.empty());

    @ParameterizedTest
    @CsvSource({ VALID + ".catalog.Catalog, true", VALID + ".catalog.spi.ProductLookup, false", "Main, false" })
    void tellsWhetherATypeLiesInTheBasePackage(String type, boolean expected) {
        // arrange
        ModuleDescriptor catalog = MODEL.module("catalog").orElseThrow();

        // act
        boolean inBasePackage = catalog.isInBasePackage(type);

        // assert
        assertThat(inBasePackage).isEqualTo(expected);
    }

    @Test
    void describesAModule() {
        // act
        ModuleDescriptor catalog = MODEL.module("catalog").orElseThrow();

        // assert
        assertThat(catalog.applicationModule().getName()).isEqualTo("catalog");
        assertThat(catalog.contains(VALID + ".catalog.spi.ProductLookup")).isTrue();
        assertThat(catalog.namedInterfaces()).singleElement().satisfies(it -> {
            assertThat(it.name()).isEqualTo("spi");
            assertThat(it.packages()).containsExactly(VALID + ".catalog.spi");
            assertThat(it.contains(VALID + ".catalog.spi.ProductLookup")).isTrue();
        });
    }

    @ParameterizedTest
    @CsvSource({ "catalog, ", "inventory, order" })
    void readsTheAllowedDependencies(String module, String expected) {
        // arrange
        ModuleDescriptor descriptor = MODEL.module(module).orElseThrow();

        // act
        Optional<List<String>> allowed = descriptor.allowedDependencies();

        // assert
        assertThat(allowed).isEqualTo(Optional.ofNullable(expected).map(List::of));
    }

    @ParameterizedTest
    @CsvSource({ "catalog, " + VALID + ".catalog.internal.CatalogProductLookup, false",
            "catalog, " + VALID + ".catalog.spi.ProductLookup, true",
            "shared, " + VALID + ".shared.util.Money, true" })
    void exposesTheTypesOfItsInterfaces(String module, String type, boolean expected) {
        // arrange
        ModuleDescriptor descriptor = MODEL.module(module).orElseThrow();

        // act
        boolean exposed = descriptor.exposes(type);

        // assert
        assertThat(exposed).isEqualTo(expected);
    }

    @Test
    void describesTheSystem() {
        // act
        String systemName = MODEL.systemName();

        // assert
        assertThat(systemName).isEqualTo("Shop");
        assertThat(MODEL.sharedModules()).containsExactly("shared");
        assertThat(Fixtures.model(EDGE, Optional.empty()).systemName()).isEqualTo("EdgeApplication");
    }

    @ParameterizedTest
    @CsvSource({ "catalog, catalog", VALID + ".catalog.internal, catalog",
            VALID + ".catalog.internal.CatalogProductLookup, catalog", "nope, ", VALID + ".ShopApplication, " })
    void resolvesModulesByNamePackageOrType(String nameOrPackage, String expected) {
        // act
        Optional<ModuleDescriptor> resolved = MODEL.resolve(nameOrPackage);

        // assert
        assertThat(resolved.map(ModuleDescriptor::name)).isEqualTo(Optional.ofNullable(expected));
    }

    @Test
    void hasNoViolationsWithoutVerification() {
        // arrange
        ModuleModel unverified = new ModuleModel(MODEL.applicationModules(), false);

        // act
        var violations = unverified.violations();

        // assert
        assertThat(violations).isEmpty();
    }

    @Test
    void rejectsTwoModulithTypes() {
        // act & assert
        assertThatThrownBy(() -> Fixtures.model("io.quarkiverse.modulith.deployment.fixtures.model.twice", Optional.empty()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only one type may be annotated with @Modulith, found: [io.quarkiverse.modulith.deployment"
                        + ".fixtures.model.twice.One, io.quarkiverse.modulith.deployment.fixtures.model.twice.Two]");
    }

    @Test
    void ignoresTheModulithTypesOfTheTestSources() {
        // arrange
        ModuleModelBuilder builder = Fixtures.builder(Fixtures.classes(TWICE)).testClasses((TWICE + ".Two")::equals);

        // act
        var data = builder.analyze().data();

        // assert
        assertThat(data.source()).isEqualTo(TWICE + ".One");
        assertThat(data.basePackage()).isEqualTo(TWICE);
    }

    @ParameterizedTest
    @CsvSource({ "'', true", "One, true", "One;Two, false" })
    void findsModulithTypesOutsideTheTestSources(String testClasses, boolean expected) {
        // arrange
        Set<String> tests = Arrays.stream(testClasses.split(";")).map(it -> TWICE + "." + it).collect(Collectors.toSet());

        // act
        boolean found = ModuleModelBuilder.hasModulithType(Fixtures.index(Fixtures.classes(TWICE)), true, tests::contains);

        // assert
        assertThat(found).isEqualTo(expected);
    }

    @Test
    void ignoresSpringAnnotationsWhenAsked() {
        // arrange
        var classes = Fixtures.classes("io.quarkiverse.modulith.deployment.fixtures.model.spring");

        // act
        var data = Fixtures.builder(classes).springAnnotations(false).analyze().data();

        // assert
        assertThat(data.basePackage()).isEqualTo("io.quarkiverse.modulith.deployment.fixtures.model.spring");
        assertThat(data.systemName()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "a.b.c.X;a.b.Y;a.b.c.d.Z, a.b", "a.b.X, a.b", "com.acme.a.A;com.acme.App;com.other.b.B, com",
            "a.X;b.Y, ", "Main;Other, ", "Main;com.acme.App, " })
    void infersTheCommonPackage(String classes, String expected) {
        // act
        Optional<String> common = ModuleModelBuilder.commonPackage(List.of(classes.split(";")));

        // assert
        assertThat(common).isEqualTo(Optional.ofNullable(expected));
    }

    @Test
    void findsNoCommonPackageWithoutClasses() {
        // act
        Optional<String> common = ModuleModelBuilder.commonPackage(List.of());

        // assert
        assertThat(common).isEmpty();
    }

    @Test
    void failsWithoutBasePackage() {
        // arrange
        ModuleModelBuilder builder = Fixtures.builder(Map.of());

        // act & assert
        assertThatThrownBy(builder::analyze).isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot determine the base package of the application. Set quarkus.modulith.base-package"
                        + " or annotate a type with @Modulith.");
    }

    @Test
    void findsNoPackageForATypeOfTheDefaultPackage() {
        // act
        String pkg = ModuleModelBuilder.packageOf("Main");

        // assert
        assertThat(pkg).isEmpty();
    }

    @Test
    void readsNothingWithoutArchives() {
        // arrange
        ArchiveClassFiles files = new ArchiveClassFiles(List.of(), true);

        // act
        files.read(null, "a.Missing");

        // assert
        assertThat(files.read("a.Missing")).isEmpty();
        assertThat(files.isTestClass("a.Missing")).isFalse();
        assertThat(files.names(List.of("", "a"))).isEmpty();
        assertThat(files.classNames()).isEmpty();
    }

    @Test
    void exposesTheClassNamesReadOnly() {
        // arrange
        Set<String> names = new ArchiveClassFiles(List.of(), true).classNames();

        // act & assert
        assertThatThrownBy(() -> names.add("a.Added")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void failsOnAMissingClassFile() {
        // arrange
        Path missing = Path.of("does-not-exist.class");

        // act & assert
        assertThatThrownBy(() -> ArchiveClassFiles.readBytes(missing)).isInstanceOf(UncheckedIOException.class);
    }

    @ParameterizedTest
    @CsvSource({ "/p/classes, false", "/p/target/test-classes, true", "/p/build/classes/java/test, true",
            "/p/build/classes/kotlin/test, true" })
    void detectsTestOutputDirectories(String root, boolean expected) {
        // act
        boolean testOutput = ArchiveClassFiles.isTestOutput(Path.of(root));

        // assert
        assertThat(testOutput).isEqualTo(expected);
    }

    @Test
    void treatsAMissingRootAsNoTestOutput() {
        // act
        boolean testOutput = ArchiveClassFiles.isTestOutput(null);

        // assert
        assertThat(testOutput).isFalse();
    }

    @ParameterizedTest
    @CsvSource({ "a, a.X;a.b.Y", "a.b, a.b.Y", "'', Main;a.X;a.b.Y;c.Z;ab.W", "c, c.Z" })
    void listsTheClassesOfThePackagesAsked(String pkg, String expected) {
        // arrange
        MapClassFiles files = new MapClassFiles(List.of("Main", "a.X", "a.b.Y", "c.Z", "ab.W"), name -> null);

        // act
        Set<String> names = files.names(List.of(pkg));

        // assert
        assertThat(names).containsExactlyInAnyOrder(expected.split(";"));
    }
}
