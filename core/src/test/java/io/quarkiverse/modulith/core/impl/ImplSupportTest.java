package io.quarkiverse.modulith.core.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ClassLocations;
import io.quarkiverse.modulith.core.fixtures.shop.ShopApplication;

class ImplSupportTest {

    @AfterEach
    void restore() {
        System.clearProperty("quarkus.modulith.verification.include-test-classes");
        System.clearProperty("quarkus.modulith.shared-modules");
    }

    @Test
    void explainsThatAsmIsNeeded() {
        // act & assert
        assertThatThrownBy(() -> ApplicationModulesLoader.requireAsm(name -> false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ApplicationModules.of(...) reads the class files with ASM. Add org.ow2.asm:asm to the test"
                        + " dependencies.");
    }

    @ParameterizedTest
    @CsvSource({ "a.b,a.b,true", "a.b,a.b.c,false", "a.b..,a.b,true", "a.b..,a.b.c,true", "a.b..,a.bc,false",
            "..b..,b,true", "..b..,x.b.y,true", "..b..,xb,false", "a..c,a.c,true", "a..c,a.x.y.c,true", "a.*,a.bee,true",
            "a.*,a.b.c,false" })
    void matchesArchUnitPackagePatterns(String pattern, String packageName, boolean matches) {
        // act
        boolean result = PackagePattern.of(pattern).matches(packageName);

        // assert
        assertThat(result).isEqualTo(matches);
    }

    @Test
    void rendersGenericTypes() {
        // arrange
        GenericType type = new GenericType("a.Map", false, ' ', 1, List.of(
                new GenericType("?", false, '*', 0, List.of()),
                new GenericType("a.Key", false, '-', 0, List.of()),
                new GenericType("T", true, '+', 0, List.of())));

        // act
        String rendered = type.render();

        // assert
        assertThat(rendered).isEqualTo("a.Map<?, ? super a.Key, ? extends T>[]");
        assertThat(type.involvedClasses()).containsExactly("a.Map", "a.Key");
        assertThat(type.argumentClasses()).containsExactly("a.Key");
    }

    @ParameterizedTest
    @CsvSource({ "La/Outer<Ljava/lang/String;>.Inner<La/Item;>;,a.Outer$Inner<a.Item>", "[I,int[]",
            "La/Plain;,a.Plain" })
    void readsFieldSignatures(String signature, String rendered) {
        // act
        Optional<GenericType> type = SignatureParts.ofType(signature);

        // assert
        assertThat(type.map(GenericType::render)).hasValue(rendered);
    }

    @Test
    void readsNoTypeWithoutFieldSignature() {
        // act
        Optional<GenericType> type = SignatureParts.ofType(null);

        // assert
        assertThat(type).isEmpty();
    }

    @Test
    void readsTheTypeParametersOfMethodSignatures() {
        // act
        SignatureParts parts = SignatureParts.of("<T::La/Iface;>(TT;)TT;");

        // assert
        assertThat(parts.typeParameters()).containsOnlyKeys("T");
        assertThat(parts.typeParameters().get("T")).extracting(GenericType::render).containsExactly("a.Iface");
        assertThat(parts.parameters()).extracting(GenericType::render).containsExactly("T");
        assertThat(parts.returnType().map(GenericType::render)).hasValue("T");
    }

    @Test
    void exposesTheSignaturePartsReadOnly() {
        // arrange
        SignatureParts parts = SignatureParts.of("Ljava/lang/Object;La/Iface;");

        // act & assert
        assertThatThrownBy(() -> parts.interfaces().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void readsPropertiesFileAndSystemProperties(@TempDir Path directory) throws IOException {
        // arrange
        System.setProperty("quarkus.modulith.shared-modules", " kernel , , legacy ");
        URLClassLoader classLoader = ServiceClassLoaders.with(directory, Map.of("application.properties",
                "quarkus.modulith.detection-strategy=explicitly-annotated\nquarkus.modulith.system-name=\nother=1\n"));

        // act
        ModulithProperties properties;
        try (classLoader) {
            properties = ModulithProperties.load(classLoader);
        }

        // assert
        assertThat(properties.get("detection-strategy")).hasValue("explicitly-annotated");
        assertThat(properties.get("system-name")).isEmpty();
        assertThat(properties.list("shared-modules")).containsExactly("kernel", "legacy");
        assertThat(properties.list("ignored-packages")).isEmpty();
        assertThat(properties.flag("spring-annotations.enabled", true)).isTrue();
        assertThat(properties.values()).doesNotContainKey("other");
        assertThat(properties.settings("a", Optional.empty(), it -> false, classLoader).sharedModules())
                .containsExactly("kernel", "legacy");
    }

    @Test
    void reportsUnreadableStreams() {
        // arrange
        InputStream broken = new BrokenStream();

        // act & assert
        assertThatThrownBy(() -> ModelAnalyzer.readAll(broken)).isInstanceOf(UncheckedIOException.class)
                .hasMessage("java.io.IOException: broken");
    }

    @Test
    void readsStreams() {
        // arrange
        InputStream stream = new ByteArrayInputStream(new byte[] { 1 });

        // act
        byte[] bytes = ModelAnalyzer.readAll(stream);

        // assert
        assertThat(bytes).containsExactly(1);
    }

    @Test
    void listsClassFilesFromJars() {
        // arrange
        ClassPathFiles files = new ClassPathFiles(getClass().getClassLoader(), it -> true);

        // act
        Set<String> names = files.names(List.of("org.jmolecules.ddd.annotation"));

        // assert
        assertThat(names).contains("org.jmolecules.ddd.annotation.AggregateRoot", "org.jmolecules.ddd.annotation.Entity")
                .allMatch(it -> it.startsWith("org.jmolecules.ddd.annotation."));
    }

    @Test
    void readsListedClassFilesFromJars() {
        // arrange
        ClassPathFiles files = new ClassPathFiles(getClass().getClassLoader(), it -> true);
        files.names(List.of("org.jmolecules.ddd.annotation"));

        // act
        Optional<byte[]> bytes = files.read("org.jmolecules.ddd.annotation.AggregateRoot");

        // assert
        assertThat(bytes).hasValueSatisfying(it -> assertThat(ClassFileScanner.scan(it).name())
                .isEqualTo("org.jmolecules.ddd.annotation.AggregateRoot"));
    }

    @Test
    void readsNothingForUnlistedClasses() {
        // arrange
        ClassPathFiles files = new ClassPathFiles(getClass().getClassLoader(), it -> true);

        // act
        Optional<byte[]> bytes = files.read("com.acme.Missing");

        // assert
        assertThat(bytes).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "file:/p/target/test-classes/a/B.class,false", "file:/p/target/classes/a/B.class,true",
            "file:/p/build/classes/java/test/a/B.class,false", "file:/p/out/test/a/B.class,false",
            "jar:file:/p/lib.jar!/a/B.class,true" })
    void recognizesTestOutputDirectories(URI location, boolean production) {
        // act
        boolean result = ClassPathFiles.isNotTestOutput(location);

        // assert
        assertThat(result).isEqualTo(production);
    }

    @ParameterizedTest
    @CsvSource({ "TESTS,file:/p/test-classes/A.class,true", "TESTS,file:/p/classes/A.class,false",
            "ALL,file:/p/classes/A.class,true", "ALL,file:/p/test-classes/A.class,true",
            "PRODUCTION,file:/p/classes/A.class,true", "PRODUCTION,file:/p/test-classes/A.class,false" })
    void filtersClassLocations(ClassLocations locations, URI location, boolean accepted) {
        // act
        boolean result = ApplicationModulesLoader.filter(locations).test(location);

        // assert
        assertThat(result).isEqualTo(accepted);
    }

    @Test
    void includesTestClassesWhenConfigured() {
        // arrange
        System.setProperty("quarkus.modulith.verification.include-test-classes", "true");

        // act
        ApplicationModules modules = ApplicationModules.of(ShopApplication.class);

        // assert
        assertThat(modules.stream()).extracting(ApplicationModule::getName).containsExactly("catalog", "kernel", "legacy",
                "catalog.pricing", "order", "inventory");
    }

    @Test
    void analysesWithTheContextClassLoaderAndLogsTheWarnings(@TempDir Path directory) throws IOException {
        // arrange
        List<String> logged = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        Logger logger = Logger.getLogger(ApplicationModulesLoader.class.getName());
        logger.addHandler(handler);

        // act
        ApplicationModules modules;
        try (URLClassLoader classLoader = ServiceClassLoaders.with(directory,
                Map.of(ServiceClassLoaders.strategies(), RegisteredStrategy.class.getName()))) {
            modules = ServiceClassLoaders.callWith(classLoader,
                    () -> ApplicationModules.of(ShopApplication.class, ClassLocations.ALL));
        } finally {
            logger.removeHandler(handler);
        }

        // assert
        assertThat(modules.stream()).extracting(ApplicationModule::getName).containsExactly("order");
        assertThat(logged).containsExactly("Registering the module detection strategy "
                + RegisteredStrategy.class.getName() + " in META-INF/services is deprecated! Set"
                + " quarkus.modulith.detection-strategy to its class name instead.");
    }

    @Test
    void createsTheJMoleculesRulesOfThePresentLibraries() {
        // act
        int all = JMoleculesRules.rules(it -> true).size();

        // assert
        assertThat(all).isEqualTo(5);
    }

    @Test
    void createsNoJMoleculesRulesWithoutTheLibraries() {
        // act
        int none = JMoleculesRules.rules(it -> false).size();

        // assert
        assertThat(none).isZero();
    }

    @ParameterizedTest
    @CsvSource({ "com.acme.Missing,false", "org.jmolecules.archunit.JMoleculesDddRules,true" })
    void detectsClassesOnTheClassPath(String name, boolean present) {
        // act
        boolean result = ClassPresence.isPresent(name);

        // assert
        assertThat(result).isEqualTo(present);
    }

    @Test
    void findsTheJMoleculesRules() {
        // act
        boolean present = ClassPresence.hasJMoleculesRules();

        // assert
        assertThat(present).isTrue();
    }

    @Test
    void resolvesNoSupertypesOfUnknownTypes() {
        // arrange
        TypePool pool = new TypePool(Map.of(), name -> Optional.empty());

        // act
        Set<String> supertypes = pool.supertypes("com.acme.Unknown");

        // assert
        assertThat(supertypes).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({ "a.A,a.A,true", "a.A,a.B,false" })
    void assignsUnknownTypesOnlyToThemselves(String type, String target, boolean assignable) {
        // arrange
        TypePool pool = new TypePool(Map.of(), name -> Optional.empty());

        // act
        boolean result = pool.isAssignableTo(type, target);

        // assert
        assertThat(result).isEqualTo(assignable);
    }

    @Test
    void resolvesNoMetaAnnotationsWithoutAnnotations() {
        // arrange
        TypePool pool = new TypePool(Map.of(), name -> Optional.empty());

        // act
        Set<String> metaAnnotations = pool.metaAnnotations(List.of());

        // assert
        assertThat(metaAnnotations).isEmpty();
    }

    @Test
    void resolvesNoSupertypesOfObject() {
        // arrange
        TypePool pool = new TypePool(Map.of(), name -> Optional.of(ModelAnalyzer.readAll(ImplSupportTest.class
                .getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class"))));

        // act
        Set<String> supertypes = pool.supertypes("java.lang.Object");

        // assert
        assertThat(supertypes).isEmpty();
    }
}
