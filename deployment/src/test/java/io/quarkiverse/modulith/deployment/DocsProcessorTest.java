package io.quarkiverse.modulith.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.deployment.docs.DocsFixture;
import io.quarkiverse.modulith.deployment.docs.DocsSettings;
import io.quarkiverse.modulith.deployment.docs.DocumentationModelBuildItem;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * Calls the documentation steps directly for the cases a build does not reach: no module model, a directory that
 * cannot be cleaned and output directories outside the build output.
 */
class DocsProcessorTest {

    @Test
    void documentsNothingWithoutModel() {
        // arrange
        List<HotDeploymentWatchedFileBuildItem> watched = new ArrayList<>();
        ModuleModelBuildItem model = new ModuleModelBuildItem(null, "");

        // act
        DocumentationModelBuildItem documentation = new DocsProcessor().documentationModel(model, null, null,
                watched::add);
        new DocsProcessor().writeDocumentation(model, documentation, null, null, item -> {
            throw new IllegalStateException("No documentation without a model");
        });

        // assert
        assertThat(documentation.getModel().modules().stream()).isEmpty();
        assertThat(watched).extracting(HotDeploymentWatchedFileBuildItem::getLocation)
                .containsExactly("META-INF/quarkus-modulith/javadoc.json");
    }

    @Test
    void replacesStaleFilesWhenCleaning(@TempDir Path temp) throws IOException {
        // arrange
        Path target = temp.resolve("target");
        Path stale = Files.writeString(Files.createDirectories(target.resolve("modulith-docs")).resolve("stale.adoc"),
                "x");
        DocsFixture fixture = new DocsFixture();
        List<GeneratedFileSystemResourceBuildItem> files = new ArrayList<>();

        // act
        DocsProcessor.write(fixture.model, fixture.build(), DocsSettings.of("modulith-docs", true), target, files::add);

        // assert
        assertThat(stale).doesNotExist();
        assertThat(files).extracting(GeneratedFileSystemResourceBuildItem::getName).contains("modulith-docs/components.puml");
    }

    @Test
    void keepsOtherFilesWithoutCleaning(@TempDir Path temp) throws IOException {
        // arrange
        Path target = temp.resolve("target");
        Path kept = Files.writeString(Files.createDirectories(target.resolve("modulith-docs")).resolve("notes.adoc"),
                "x");
        DocsFixture fixture = new DocsFixture();

        // act
        DocsProcessor.write(fixture.model, fixture.build(), DocsSettings.of("modulith-docs", false), target, it -> {
        });

        // assert
        assertThat(kept).hasContent("x");
    }

    @Test
    void cleansOnlyExistingDirectories(@TempDir Path temp) throws IOException {
        // arrange
        Path directory = Files.createDirectories(temp.resolve("docs/sub"));
        Files.writeString(directory.resolve("file.adoc"), "x");

        // act
        DocsProcessor.clean(temp.resolve("missing"));
        DocsProcessor.clean(temp.resolve("docs"));

        // assert
        assertThat(temp.resolve("docs")).isEmptyDirectory();
        assertThat(temp.resolve("missing")).doesNotExist();
    }

    @Test
    void reportsDirectoriesThatCannotBeCleaned(@TempDir Path temp) throws IOException {
        // arrange
        Path directory = Files.createDirectories(temp.resolve("locked"));
        Files.writeString(directory.resolve("file.adoc"), "x");
        directory.toFile().setWritable(false);
        assumeFalse(Files.isWritable(directory), "the user can write read-only directories, as root does");

        // act
        Throwable thrown = catchThrowable(() -> DocsProcessor.clean(directory));
        directory.toFile().setWritable(true);

        // assert
        assertThat(thrown).isInstanceOf(UncheckedIOException.class)
                .hasMessage("Cannot clean the module documentation directory " + directory);
    }

    @ParameterizedTest
    @ValueSource(strings = { "modulith-docs", "docs/modulith", "docs/../modulith-docs", "./modulith-docs" })
    void resolvesDirectoriesInsideTheBuildOutput(String configured, @TempDir Path temp) {
        // arrange
        Path target = temp.resolve("project/target");

        // act
        Path directory = DocsProcessor.documentationDirectory(target, configured);

        // assert
        assertThat(directory).isEqualTo(target.toAbsolutePath().normalize().resolve(configured).normalize());
        assertThat(directory.getParent().toString()).startsWith(target.toAbsolutePath().normalize().toString());
    }

    @ParameterizedTest
    @ValueSource(strings = { "..", "../src/docs", "../..", "docs/../..", ".", "", "/", "/home/user/docs" })
    void rejectsDirectoriesOutsideTheBuildOutput(String configured, @TempDir Path temp) {
        // arrange
        Path target = temp.resolve("project/target").toAbsolutePath().normalize();

        // act
        Throwable thrown = catchThrowable(() -> DocsProcessor.documentationDirectory(target, configured));

        // assert
        assertThat(thrown).isInstanceOf(ConfigurationException.class)
                .hasMessage("quarkus.modulith.docs.output-directory must name a sub-directory of the build output "
                        + target + ", but '" + configured + "' resolves to " + target.resolve(configured).normalize());
    }

    @ParameterizedTest
    @ValueSource(strings = { "classes", "classes/docs", "test-classes", "quarkus-app/docs", "generated-sources" })
    void rejectsDirectoriesOfTheBuild(String configured, @TempDir Path temp) {
        // arrange
        Path target = temp.resolve("project/target").toAbsolutePath().normalize();

        // act
        Throwable thrown = catchThrowable(() -> DocsProcessor.documentationDirectory(target, configured));

        // assert
        assertThat(thrown).isInstanceOf(ConfigurationException.class)
                .hasMessage("quarkus.modulith.docs.output-directory must not lie in " + configured.split("/")[0]
                        + ", a directory of the build that cleaning would damage, but '" + configured + "' resolves to "
                        + target.resolve(configured) + ". Use a directory of its own, such as the default modulith-docs");
    }

    @Test
    void acceptsAnAbsoluteDirectoryInsideTheBuildOutput(@TempDir Path temp) {
        // arrange
        Path target = temp.resolve("project/target").toAbsolutePath().normalize();
        String configured = target.resolve("docs").toString();

        // act
        Path directory = DocsProcessor.documentationDirectory(target, configured);

        // assert
        assertThat(directory).isEqualTo(target.resolve("docs"));
    }

    @Test
    void removesLinksWithoutFollowingThem(@TempDir Path temp) throws IOException {
        // arrange
        Path outside = Files.writeString(Files.createDirectories(temp.resolve("outside")).resolve("keep.txt"), "x");
        Path docs = Files.createDirectories(temp.resolve("target/modulith-docs"));
        Files.createSymbolicLink(docs.resolve("link"), outside.getParent());

        // act
        DocsProcessor.clean(docs);

        // assert
        assertThat(docs).isEmptyDirectory();
        assertThat(outside).hasContent("x");
    }

    @Test
    void neverCleansOutsideTheBuildOutput(@TempDir Path temp) throws IOException {
        // arrange
        Path source = Files.writeString(Files.createDirectories(temp.resolve("project/src")).resolve("Main.java"), "x");
        Path target = Files.createDirectories(temp.resolve("project/target"));

        // act
        Throwable thrown = catchThrowable(() -> DocsProcessor.clean(DocsProcessor.documentationDirectory(target, "..")));

        // assert
        assertThat(thrown).isInstanceOf(ConfigurationException.class);
        assertThat(source).hasContent("x");
    }
}
