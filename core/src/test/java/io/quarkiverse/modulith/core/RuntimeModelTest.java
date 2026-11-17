package io.quarkiverse.modulith.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.core.ModelData.ModuleData;
import io.quarkiverse.modulith.core.ModelData.NamedInterfaceData;
import io.quarkiverse.modulith.core.ModelData.ViolationData;
import io.quarkiverse.modulith.core.data.ModelCodec;
import io.quarkiverse.modulith.core.fixtures.rules.RulesApplication;
import io.quarkiverse.modulith.core.fixtures.shop.ShopApplication;
import io.quarkiverse.modulith.core.util.ApplicationModulesExporter;

class RuntimeModelTest {

    static final ApplicationModules SHOP = ApplicationModules.of(ShopApplication.class, ClassLocations.TESTS);

    static final ModuleData ORDER = new ModuleData("order", "Orders", "com.acme.order", false, false, List.of(),
            List.of(new NamedInterfaceData("events", List.of("com.acme.order.events"), List.of())), List.of(),
            List.of("com.acme.order.OrderPlaced"), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of());
    static final ApplicationModules UNTYPED = new ApplicationModules(new ModelData("Shop", "com.acme", List.of(ORDER),
            List.of(new ViolationData("CYCLE", "a", "a -> b -> a", "", "cycle")), ""));
    static final String INCLUDE_TEST_CLASSES = "quarkus.modulith.verification.include-test-classes";

    @AfterEach
    void clearProperties() {
        System.clearProperty(INCLUDE_TEST_CLASSES);
    }

    @ParameterizedTest
    @CsvSource({ "com.acme.order.internal.Repo,order", "com.acme.order.OrderPlaced,order", "com.other.Type," })
    void findsModulesByPackageWithoutTypeInformation(String type, String module) {
        // act
        Optional<ApplicationModule> result = UNTYPED.getModuleForType(type);

        // assert
        assertThat(result.map(ApplicationModule::getName)).isEqualTo(Optional.ofNullable(module));
    }

    @Test
    void readsTheRecordedViolations() {
        // act
        Violations violations = UNTYPED.detectViolations();

        // assert
        assertThat(violations.getMessages()).containsExactly("cycle");
    }

    @Test
    void readsTheRootPackages() {
        // act
        List<JavaPackage> roots = UNTYPED.getRootPackages();

        // assert
        assertThat(roots).extracting(JavaPackage::getName).containsExactly("com.acme");
    }

    @Test
    void hasNoRootPackageWithoutBasePackage() {
        // act
        List<JavaPackage> roots = new ApplicationModules(new ModelData("", "", List.of(), List.of(), "")).getRootPackages();

        // assert
        assertThat(roots).isEmpty();
    }

    @Test
    void readsTheNamedInterfacesWithoutTypes() {
        // act
        Set<String> interfaces = UNTYPED.getModuleByName("order").orElseThrow().getNamedInterfaces();

        // assert
        assertThat(interfaces).containsExactly("events");
    }

    @Test
    void returnsTheRecordedDiagram() {
        // arrange
        ApplicationModules modules = new ApplicationModules(new ModelData("", "", List.of(), List.of(), "flowchart TB"));

        // act
        String mermaid = modules.toMermaid();

        // assert
        assertThat(mermaid).isEqualTo("flowchart TB");
    }

    @Test
    void recordsAndRestoresTheModel() {
        // arrange
        ModelData data = SHOP.getModelData();

        // act
        ModelData restored = ModelCodec.decode(ModelCodec.encode(data));

        // assert
        assertThat(restored).isEqualTo(data);
    }

    @Test
    void replacesTheDiagramAndKeepsTheVerificationState() {
        // act
        ModelData data = SHOP.getModelData().withMermaid("flowchart");

        // assert
        assertThat(data.mermaid()).isEqualTo("flowchart");
        assertThat(data.verified()).isFalse();
    }

    @Test
    void marksAModelWithViolationsAsVerified() {
        // act
        ModelData data = SHOP.getModelData().withViolations(List.of(), "m");

        // assert
        assertThat(data.verified()).isTrue();
        assertThat(data.mermaid()).isEqualTo("m");
    }

    @Test
    void combinesTheModelAndTheRuntime() {
        // arrange
        ApplicationRuntime runtime = new FixedRuntime();

        // act
        ApplicationModulesRuntime modulesRuntime = new ApplicationModulesRuntime(SHOP, runtime);

        // assert
        assertThat(modulesRuntime.get()).isSameAs(SHOP);
        assertThat(modulesRuntime.getRuntime()).isSameAs(runtime);
        assertThat(modulesRuntime.getUserClass("bean", "name")).isEqualTo(String.class);
    }

    @ParameterizedTest
    @CsvSource({ "java.lang.String,true", "io.quarkiverse.modulith.core.fixtures.shop.catalog.internal.Stock,true",
            "io.quarkiverse.modulith.core.RuntimeModelTest,false" })
    void tellsApplicationClasses(Class<?> type, boolean application) {
        // arrange
        ApplicationModulesRuntime modulesRuntime = new ApplicationModulesRuntime(SHOP, new FixedRuntime());

        // act
        boolean result = modulesRuntime.isApplicationClass(type);

        // assert
        assertThat(result).isEqualTo(application);
    }

    @Test
    void looksUpTheModulesFactory() {
        // act
        ApplicationModulesFactory factory = ApplicationModulesFactory.lookup();

        // assert
        assertThat(factory.of(ShopApplication.class).getSystemName()).isEqualTo("Shop");
    }

    @Test
    void buildsModelsWithTheDefaultFactory() {
        // act
        ApplicationModules modules = ApplicationModulesFactory.defaultFactory().of(ShopApplication.class);

        // assert
        assertThat(modules.getSource()).isEqualTo(ShopApplication.class.getName());
    }

    @Test
    void exportsTheModelAsJson() {
        // arrange
        ApplicationModulesExporter exporter = new ApplicationModulesExporter(SHOP);

        // act
        String json = exporter.toJson();

        // assert
        assertThat(json).startsWith("{ \"catalog\" : { \"displayName\" : \"Catalog\", \"basePackage\" : "
                + "\"io.quarkiverse.modulith.core.fixtures.shop.catalog\", \"nested\" : [ \"catalog.pricing\" ], \"type\" :"
                + " \"closed\", \"shared\" : false, \"dependencies\" : [] }")
                .contains("\"kernel\" : { \"displayName\" : \"Shared kernel\", \"basePackage\" : "
                        + "\"io.quarkiverse.modulith.core.fixtures.shop.kernel\", \"nested\" : [], \"type\" : \"open\","
                        + " \"shared\" : true, \"dependencies\" : [] }")
                .contains("\"parent\" : \"catalog\"")
                .contains("\"allowedDependencies\" : [ \"catalog :: api\" ]")
                .contains("\"types\" : [ \"USES_COMPONENT\", \"EVENT_LISTENER\" ]");
    }

    @Test
    void exportsTheNamedInterfacesAndInitializers() {
        // arrange
        ApplicationModulesExporter exporter = new ApplicationModulesExporter(SHOP);

        // act
        String json = exporter.toFullJson();

        // assert
        assertThat(json).contains("\"initializers\" : [ \"io.quarkiverse.modulith.core.fixtures.shop"
                + ".inventory.InventoryInitializer\" ]",
                "\"api\" : [ \"io.quarkiverse.modulith.core.fixtures.shop.catalog.api.Products\" ]");
    }

    @Test
    void writesTheExportToAFile(@TempDir Path directory) throws Exception {
        // arrange
        System.setProperty(INCLUDE_TEST_CLASSES, "true");
        String expected = new ApplicationModulesExporter(ApplicationModules.of(ShopModelTest.BASE)).toFullJson();
        Path file = directory.resolve("modules.json");

        // act
        ApplicationModulesExporter.main(new String[] { ShopModelTest.BASE, file.toString() });

        // assert
        assertThat(Files.readString(file)).isEqualTo(expected).startsWith("{ \"catalog\" : {");
    }

    @Test
    void explainsTheUsageOfTheExporter() {
        // act & assert
        assertThatThrownBy(() -> ApplicationModulesExporter.main(new String[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usage: ApplicationModulesExporter <base package> [output file]");
    }

    @Test
    void exportsTheDeclarationsOfInvalidModules() {
        // arrange
        ApplicationModules rules = ApplicationModules.of(RulesApplication.class, ClassLocations.TESTS);

        // act
        String json = new ApplicationModulesExporter(rules).toJson();

        // assert
        assertThat(json).contains("\"allowedDependencies\" : [ \"ghost\", \"store::nope\", \"store\" ]");
    }
}
