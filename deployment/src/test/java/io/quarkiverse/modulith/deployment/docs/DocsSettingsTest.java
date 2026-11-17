package io.quarkiverse.modulith.deployment.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.core.DependencyDepth;
import io.quarkiverse.modulith.deployment.model.Fixtures;
import io.quarkiverse.modulith.deployment.model.ModuleModel;
import io.quarkiverse.modulith.docs.DiagramStyle;
import io.quarkiverse.modulith.docs.ElementsWithoutRelationships;
import io.quarkus.paths.PathVisit;
import io.quarkus.runtime.LaunchMode;

class DocsSettingsTest {

    static final String CYCLE = "io.quarkiverse.modulith.deployment.fixtures.cycle";
    static final String VIOLATION = "Cycle detected: a -> b -> a   1. Dependencies of a     - Field <" + CYCLE
            + ".a.A.b> has type <" + CYCLE + ".b.B> in (A.java:0)   2. Dependencies of b     - Field <" + CYCLE
            + ".b.B.a> has type <" + CYCLE + ".a.A> in (B.java:0)";

    static final DiagramSettings DIAGRAM = new DiagramSettings(DiagramStyle.C4, DependencyDepth.IMMEDIATE,
            Optional.empty(), ElementsWithoutRelationships.HIDDEN);

    @ParameterizedTest
    @CsvSource({
            "true, false, NORMAL, true",
            "false, true, NORMAL, false",
            "true, false, DEVELOPMENT, false",
            "true, true, DEVELOPMENT, true",
            "false, true, DEVELOPMENT, false",
            "true, true, TEST, false" })
    void writesWhenEnabledForTheLaunchMode(boolean enabled, boolean devMode, LaunchMode mode, boolean active) {
        // arrange
        DocsConfig config = new DocsSettings(enabled, devMode, "modulith-docs", true, DIAGRAM, canvas(
                CanvasGroupings.QUARKUS));

        // act
        boolean actual = new DocsActive(config, mode).getAsBoolean();

        // assert
        assertThat(actual).isEqualTo(active);
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = { "QUARKUS;|_REST resources_", "SPRING;|_Controllers_",
            "NONE;|* `i.q.m.d.d.f.a.w.Billing`" })
    void choosesTheCanvasGroupings(CanvasGroupings groupings, String expected) {
        // arrange
        DocsConfig.Canvas config = canvas(groupings);
        DocsFixture fixture = new DocsFixture();

        // act
        Map<String, String> files = DocumentationWriter.render(fixture.model, fixture.build(),
                DocsOptions.diagram(DIAGRAM), DocsOptions.canvas(config));

        // assert
        assertThat(files.get("module-web.adoc")).contains(expected);
    }

    @Test
    void writesViolationsInTheOverview() {
        // arrange
        ModuleModel model = Fixtures.model("io.quarkiverse.modulith.deployment.fixtures.cycle",
                Optional.of("io.quarkiverse.modulith.deployment.fixtures.cycle"));

        // act
        String overview = DocumentationWriter.overview(model);

        // assert
        assertThat(overview).contains("\n## Violations\n\n1. " + VIOLATION + "\n", "| any |");
    }

    @Test
    void drawsOpenModulesOnlyWhenThereAreSome() {
        // arrange
        ModuleModel closed = Fixtures.model("io.quarkiverse.modulith.deployment.fixtures.cycle",
                Optional.of("io.quarkiverse.modulith.deployment.fixtures.cycle"));
        ModuleModel valid = Fixtures.model("io.quarkiverse.modulith.deployment.fixtures.valid", Optional.empty());

        // act
        String mermaid = Diagrams.mermaid(closed);
        String overview = DocumentationWriter.overview(valid);

        // assert
        assertThat(mermaid).doesNotContain("classDef");
        assertThat(overview).contains("| Inventory | `io.quarkiverse.modulith.deployment.fixtures.valid.inventory` | closed "
                + "| order | order |").doesNotContain("## Violations");
    }

    @Test
    void readsTypesFromTheIndex() throws IOException {
        // arrange
        TypeInspector types = new TypeInspector(Index.of(Object.class, String.class, CharSequence.class));

        // act
        Set<String> supertypes = types.supertypes(types.classInfo("java.lang.String").orElseThrow());
        Set<String> objectSupertypes = types.supertypes(types.classInfo("java.lang.Object").orElseThrow());

        // assert
        assertThat(supertypes).contains("java.lang.Object", "java.lang.CharSequence", "java.io.Serializable");
        assertThat(objectSupertypes).isEmpty();
    }

    @Test
    void findsNoPackageForATypeOfTheDefaultPackage() {
        // act
        String pkg = TypeInspector.packageOf("Simple");

        // assert
        assertThat(pkg).isEmpty();
    }

    @Test
    void readsArchiveResourcesDefensively(@TempDir Path temp) {
        // arrange
        Path missing = temp.resolve("missing.class");

        // act
        byte[] none = ArchiveResources.read((PathVisit) null);
        Throwable thrown = catchThrowable(() -> ArchiveResources.read(missing));

        // assert
        assertThat(none).isNull();
        assertThat(thrown).isInstanceOf(UncheckedIOException.class).hasMessage("Cannot read " + missing);
    }

    @Test
    void findsNoResourceWithoutArchives() {
        // arrange
        ArchiveResources resources = new ArchiveResources(List.of());

        // act
        List<byte[]> found = resources.all("com/acme/Missing.class");

        // assert
        assertThat(found).isEmpty();
    }

    static CanvasSettings canvas(CanvasGroupings groupings) {
        return new CanvasSettings(groupings, false, false, Optional.empty());
    }
}
