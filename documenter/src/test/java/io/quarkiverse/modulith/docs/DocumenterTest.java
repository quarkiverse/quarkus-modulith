package io.quarkiverse.modulith.docs;

import static io.quarkiverse.modulith.docs.TestModels.module;
import static io.quarkiverse.modulith.docs.TestModels.modules;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ClassLocations;
import io.quarkiverse.modulith.docs.fixtures.shop.ShopApplication;

class DocumenterTest {

    static final ApplicationModules SHOP = ApplicationModules.of(ShopApplication.class, ClassLocations.TESTS);

    private Path locked;

    @AfterEach
    void unlock() {
        Optional.ofNullable(locked).ifPresent(it -> it.toFile().setWritable(true));
    }

    @ParameterizedTest
    @CsvSource({ "shop, C4, false", "shop-uml, UML, true" })
    void writesTheDocumentationOfTheFixture(String golden, DiagramStyle style, boolean excludeAll, @TempDir Path temp) {
        // arrange
        DocumentationModel model = new ParityFixture(ShopApplication.class).model;
        Documenter documenter = new Documenter(model, DocumenterOptions.defaults().withOutputFolder(
                temp.resolve("docs").toString()), temp.resolve("classes"));
        DiagramOptions options = DiagramOptions.defaults().withStyle(style).withExclusions(it -> excludeAll);

        // act
        documenter.writeDocumentation(options, CanvasOptions.defaults());

        // assert
        assertThat(read(temp.resolve("docs"))).isEqualTo(read(golden(golden)));
        assertThat(read(temp.resolve("classes"))).isEqualTo(read(golden("shop-metadata")));
    }

    @Test
    void writesTheDocumentationOfTheModuleModelAlone(@TempDir Path temp) {
        // arrange
        Documenter documenter = new Documenter(DocumentationModel.of(SHOP),
                DocumenterOptions.defaults().withOutputFolder(temp.resolve("docs").toString()), temp.resolve("classes"));

        // act
        documenter.writeDocumentation(DiagramOptions.defaults().withExclusions(it -> true), CanvasOptions.defaults());

        // assert
        assertThat(read(temp.resolve("docs"))).isEqualTo(read(golden("derived")));
        assertThat(read(temp.resolve("classes"))).isEqualTo(read(golden("derived-metadata")));
    }

    @Test
    void writesWithDefaultOptionsMethodByMethod(@TempDir Path temp) {
        // arrange
        DocumentationModel model = new ParityFixture(ShopApplication.class).model;
        Documenter documenter = new Documenter(model, DocumenterOptions.defaults().withOutputFolder(temp.toString()));
        ApplicationModule order = model.modules().getModuleByName("order").orElseThrow();

        // act
        documenter.writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleAsPlantUml(order)
                .writeModuleCanvases()
                .writeAggregatingDocument();

        // assert
        assertThat(read(temp)).isEqualTo(read(golden("shop")));
        assertThat(documenter.getModel()).isSameAs(model);
        assertThat(documenter.getModules()).isSameAs(model.modules());
    }

    @Test
    void writesEverythingWithDefaultOptions(@TempDir Path temp) {
        // arrange
        DocumentationModel model = new ParityFixture(ShopApplication.class).model;
        Documenter documenter = new Documenter(model, DocumenterOptions.defaults().withOutputFolder(
                temp.resolve("docs").toString()), temp.resolve("classes"));

        // act
        documenter.writeDocumentation();

        // assert
        assertThat(read(temp.resolve("docs"))).isEqualTo(read(golden("shop")));
    }

    @Test
    void cleansTheOutputOnceBeforeTheFirstWrite(@TempDir Path temp) throws IOException {
        // arrange
        Files.writeString(temp.resolve("stale.txt"), "stale");
        Documenter documenter = new Documenter(DocumentationModel.of(DocumentationModelTest.MODULES),
                DocumenterOptions.defaults().withOutputFolder(temp.toString()));

        // act
        documenter.writeModulesAsPlantUml();
        Files.writeString(temp.resolve("fresh.txt"), "fresh");
        documenter.writeModuleCanvases();

        // assert
        assertThat(read(temp).keySet()).containsExactly("components.puml", "fresh.txt", "module-catalog.adoc",
                "module-inventory.adoc", "module-order.adoc");
    }

    @Test
    void keepsTheOutputWithoutClean(@TempDir Path temp) throws IOException {
        // arrange
        Files.writeString(temp.resolve("kept.txt"), "kept");
        Documenter documenter = new Documenter(DocumentationModel.of(DocumentationModelTest.MODULES),
                DocumenterOptions.defaults().withOutputFolder(temp.toString()).withoutClean());

        // act
        documenter.writeModulesAsPlantUml();

        // assert
        assertThat(read(temp).keySet()).containsExactly("components.puml", "kept.txt");
    }

    @Test
    void writesNoAggregatingDocumentWithoutDiagramsOrCanvases(@TempDir Path temp) {
        // arrange
        Documenter documenter = new Documenter(DocumentationModel.of(DocumentationModelTest.MODULES),
                DocumenterOptions.defaults().withOutputFolder(temp.toString()));

        // act
        documenter.writeAggregatingDocument();

        // assert
        assertThat(read(temp)).isEmpty();
    }

    @Test
    void rejectsDuplicateComponentNames(@TempDir Path temp) {
        // arrange
        Documenter documenter = new Documenter(DocumentationModel.of(DocumentationModelTest.MODULES),
                DocumenterOptions.defaults().withOutputFolder(temp.toString()));

        // act
        Throwable thrown = catchThrowable(
                () -> documenter.writeModulesAsPlantUml(DiagramOptions.defaults().withDefaultDisplayName(it -> "Same")));

        // assert
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A component named 'Same' already exists for this container.");
    }

    @Test
    void rejectsModulesOfAnotherApplication(@TempDir Path temp) {
        // arrange
        Documenter documenter = new Documenter(DocumentationModel.of(DocumentationModelTest.MODULES),
                DocumenterOptions.defaults().withOutputFolder(temp.toString()));
        ApplicationModules other = modules("Other", module("billing", List.of()));

        // act
        Throwable thrown = catchThrowable(
                () -> documenter.writeModuleAsPlantUml(other.getModuleByName("billing").orElseThrow()));

        // assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessage("Couldn't find component for module # BILLING\n> Logical name: billing\n"
                        + "> Base package: com.acme.billing\n> Excluded packages: none\n> Beans: none\n!");
    }

    @Test
    void createsTheModelFromTheModules() {
        // act
        Documenter documenter = new Documenter(SHOP);

        // assert
        assertThat(documenter.getModules()).isSameAs(SHOP);
        assertThat(documenter.getModel().details(SHOP.getModuleByName("order").orElseThrow()).publishedEvents())
                .extracting(it -> it.type().name()).containsExactly(DocumentationModelTest.SHOP + "order.OrderCancelled",
                        DocumentationModelTest.SHOP + "order.OrderCompleted");
    }

    @Test
    void analyzesTheTypeOnFirstUse() {
        // act
        Documenter documenter = new Documenter((Class<?>) null);
        Throwable thrown = catchThrowable(documenter::getModules);

        // assert
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void analyzesTheProductionClassesOfAType() {
        // act
        Documenter documenter = new Documenter(ShopApplication.class);

        // assert
        assertThat(documenter.getModules().getBasePackage()).isEqualTo(ShopApplication.class.getPackageName());
        assertThat(documenter.getModules().stream()).isEmpty();
        assertThat(documenter.getModel().modules()).isSameAs(documenter.getModules());
    }

    @Test
    void reportsUnwritableOutput(@TempDir Path temp) throws IOException {
        // arrange
        Path file = Files.writeString(temp.resolve("file"), "x");
        FolderOutput output = new FolderOutput(file.resolve("docs"));

        // act
        Throwable thrown = catchThrowable(() -> output.write("components.puml", "x"));

        // assert
        assertThat(thrown).isInstanceOf(UncheckedIOException.class)
                .hasMessage("Cannot write " + file.resolve("docs/components.puml"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "../escaped.puml", "nested/../../escaped.puml" })
    void refusesFileNamesLeavingTheOutput(String name, @TempDir Path temp) {
        // arrange
        FolderOutput output = new FolderOutput(temp.resolve("docs"));

        // act
        Throwable thrown = catchThrowable(() -> output.write(name, "x"));

        // assert
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File name " + name + " leaves the documentation output " + temp.resolve("docs"));
        assertThat(temp.resolve("escaped.puml")).doesNotExist();
    }

    @Test
    void refusesAnAbsoluteFileName(@TempDir Path temp) {
        // arrange
        FolderOutput output = new FolderOutput(temp.resolve("docs"));
        String name = temp.resolve("escaped.puml").toString();

        // act
        Throwable thrown = catchThrowable(() -> output.write(name, "x"));

        // assert
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File name " + name + " leaves the documentation output " + temp.resolve("docs"));
    }

    @Test
    void reportsUndeletableOutput(@TempDir Path temp) throws IOException {
        // arrange
        Path folder = Files.createDirectories(temp.resolve("locked"));
        Files.writeString(folder.resolve("file"), "x");
        FolderOutput output = new FolderOutput(folder);
        locked = folder;
        assumeTrue(folder.toFile().setWritable(false) && !Files.isWritable(folder),
                "the user can delete from read-only folders, as root can");

        // act
        Throwable thrown = catchThrowable(output::clear);

        // assert
        assertThat(thrown).isInstanceOf(UncheckedIOException.class).hasMessage("Cannot delete " + folder);
        assertThat(output.folder()).isEqualTo(folder);
    }

    @Test
    void clearsAMissingFolderSilently(@TempDir Path temp) {
        // arrange
        FolderOutput output = new FolderOutput(temp.resolve("missing"));

        // act
        output.clear();

        // assert
        assertThat(output.contains("anything")).isFalse();
        assertThat(Files.exists(temp.resolve("missing"))).isFalse();
    }

    static Path golden(String name) {
        return Path.of("src/test/resources/golden", name);
    }

    static Map<String, String> read(Path folder) {
        Map<String, String> result = new TreeMap<>();
        if (!Files.exists(folder)) {
            return result;
        }
        try (Stream<Path> files = Files.walk(folder)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                result.put(folder.relativize(file).toString().replace('\\', '/'),
                        Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n"));
            }
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
