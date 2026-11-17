package io.quarkiverse.modulith.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.deployment.docs.ArchiveResources;
import io.quarkiverse.modulith.deployment.docs.DocsActive;
import io.quarkiverse.modulith.deployment.docs.DocsConfig;
import io.quarkiverse.modulith.deployment.docs.DocsOptions;
import io.quarkiverse.modulith.deployment.docs.DocumentationModelBuildItem;
import io.quarkiverse.modulith.deployment.docs.DocumentationModelBuilder;
import io.quarkiverse.modulith.deployment.docs.DocumentationWriter;
import io.quarkiverse.modulith.deployment.model.ModuleModel;
import io.quarkiverse.modulith.docs.DocumentationModel;
import io.quarkiverse.modulith.docs.metadata.DocumentationSource;
import io.quarkiverse.modulith.docs.metadata.TypeMetadata;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedFileSystemResourceBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * Builds the documentation model and writes the module documentation when the application is packaged.
 */
class DocsProcessor {

    /**
     * The directories Maven, Gradle and Quarkus write in the build output.
     */
    static final Set<String> BUILD_DIRECTORIES = Set.of("classes", "test-classes", "generated-sources",
            "generated-test-sources", "maven-status", "maven-archiver", "surefire-reports", "failsafe-reports",
            "quarkus-app", "quarkus-build", "lib", "libs", "resources", "tmp", "generated", "kotlin");

    @BuildStep
    DocumentationModelBuildItem documentationModel(ModuleModelBuildItem model, CombinedIndexBuildItem index,
            ApplicationArchivesBuildItem archives, BuildProducer<HotDeploymentWatchedFileBuildItem> watched) {
        watched.produce(new HotDeploymentWatchedFileBuildItem(DocumentationSource.LOCATION));
        if (model.model().isEmpty()) {
            return new DocumentationModelBuildItem(new DocumentationModel(new ApplicationModules(model.getModelData()),
                    List.of(), List.of(), DocumentationSource.none()));
        }
        ModuleModel moduleModel = model.model().get();
        ApplicationModules modules = moduleModel.applicationModules();
        List<ApplicationArchive> all = new ArrayList<>();
        all.add(archives.getRootArchive());
        all.addAll(archives.getArchives());
        ArchiveResources resources = new ArchiveResources(all);
        List<TypeMetadata> javadoc = new ArrayList<>();
        resources.all(DocumentationSource.LOCATION)
                .forEach(bytes -> javadoc.addAll(DocumentationSource.parse(new String(bytes, StandardCharsets.UTF_8))));
        return new DocumentationModelBuildItem(new DocumentationModelBuilder(index.getIndex())
                .build(moduleModel, modules, DocumentationSource.of(javadoc)));
    }

    @BuildStep(onlyIf = DocsActive.class)
    void writeDocumentation(ModuleModelBuildItem model, DocumentationModelBuildItem documentation, DocsConfig config,
            OutputTargetBuildItem output, BuildProducer<GeneratedFileSystemResourceBuildItem> files) {
        model.model().ifPresent(it -> write(it, documentation.getModel(), config, output.getOutputDirectory(), files));
    }

    /**
     * Cleans the documentation directory when configured and produces the documentation files.
     */
    static void write(ModuleModel model, DocumentationModel documentation, DocsConfig config, Path buildOutput,
            BuildProducer<GeneratedFileSystemResourceBuildItem> files) {
        Path directory = documentationDirectory(buildOutput, config.outputDirectory());
        if (config.clean()) {
            clean(directory);
        }
        DocumentationWriter.render(model, documentation, DocsOptions.diagram(config.diagram()),
                DocsOptions.canvas(config.canvas())).forEach(
                        (name, content) -> files.produce(
                                new GeneratedFileSystemResourceBuildItem(
                                        Path.of(config.outputDirectory()).resolve(name).toString(),
                                        content.getBytes(StandardCharsets.UTF_8))));
    }

    /**
     * Resolves the documentation directory and makes sure it lies strictly inside the build output and outside the
     * directories the build tools and Quarkus write, so that cleaning it can never delete sources, compiled classes or
     * the packaged application: an absolute path outside the build output, {@code ..}, {@code .} or a directory such as
     * {@code classes} fails the build.
     *
     * @param buildOutput the build output directory, {@code target} with Maven
     * @param configured the value of {@code quarkus.modulith.docs.output-directory}
     */
    static Path documentationDirectory(Path buildOutput, String configured) {
        Path root = buildOutput.toAbsolutePath().normalize();
        Path directory = root.resolve(configured).normalize();
        if (directory.equals(root) || !directory.startsWith(root)) {
            throw new ConfigurationException("quarkus.modulith.docs.output-directory must name a sub-directory of the "
                    + "build output " + root + ", but '" + configured + "' resolves to " + directory);
        }
        String first = root.relativize(directory).getName(0).toString();
        if (BUILD_DIRECTORIES.contains(first)) {
            throw new ConfigurationException("quarkus.modulith.docs.output-directory must not lie in " + first
                    + ", a directory of the build that cleaning would damage, but '" + configured + "' resolves to "
                    + directory + ". Use a directory of its own, such as the default modulith-docs");
        }
        return directory;
    }

    /**
     * Deletes the content of the documentation directory, the one side effect of this processor outside build items:
     * Quarkus has no build item to remove files of the build output.
     */
    static void clean(Path directory) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).filter(it -> !it.equals(directory)).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot clean the module documentation directory " + directory, e);
        }
    }
}
