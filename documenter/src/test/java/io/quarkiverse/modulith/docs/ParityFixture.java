package io.quarkiverse.modulith.docs;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ClassLocations;
import io.quarkiverse.modulith.docs.metadata.DocumentationSource;

/**
 * A fixture application analyzed by Spring Modulith 2.1.2, and the same application analyzed by the Quarkus Modulith
 * core and derived through {@link DocumentationModel#of}, with the build-only configuration properties added.
 */
final class ParityFixture {

    static final String JAVADOC = javadoc();

    final org.springframework.modulith.core.ApplicationModules spring;
    final DocumentationModel model;

    ParityFixture(Class<?> application) {
        spring = org.springframework.modulith.core.ApplicationModules.of(application, location -> true);
        model = withProperties(DocumentationModel.of(ApplicationModules.of(application, ClassLocations.TESTS)));
    }

    private static DocumentationModel withProperties(DocumentationModel derived) {
        ApplicationModules modules = derived.modules();
        List<ModuleDetails> details = modules.stream().map(module -> {
            ModuleDetails it = derived.details(module);
            return new ModuleDetails(it.identifier(), it.parent(), it.types(), it.beans(), it.dependencies(),
                    it.publishedEvents(), it.aggregateRoots(), it.valueTypes(), SpringParityTest.PROPERTIES.stream()
                            .filter(property -> module.getType(property.sourceType()).isPresent()).toList(),
                    it.namedInterfaces(), it.initializers());
        }).toList();
        return new DocumentationModel(modules, details,
                derived.sharedModules().stream().map(ApplicationModule::getName).toList(),
                DocumentationSource.of(DocumentationSource.parse(JAVADOC)));
    }

    private static String javadoc() {
        try (InputStream in = ParityFixture.class.getResourceAsStream("/fixture-javadoc.json")) {
            return new String(Objects.requireNonNull(in, "fixture-javadoc.json").readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void spring(Path folder, org.springframework.modulith.docs.Documenter.DiagramOptions diagram,
            org.springframework.modulith.docs.Documenter.CanvasOptions canvas) {
        new org.springframework.modulith.docs.Documenter(spring,
                org.springframework.modulith.docs.Documenter.Options.defaults().withOutputFolder(folder.toString()))
                .writeModulesAsPlantUml(diagram)
                .writeIndividualModulesAsPlantUml(diagram)
                .writeModuleCanvases(canvas)
                .writeAggregatingDocument(diagram, canvas);
    }

    void quarkus(Path folder, DiagramOptions diagram, CanvasOptions canvas) {
        new Documenter(model, DocumenterOptions.defaults().withOutputFolder(folder.toString()))
                .writeModulesAsPlantUml(diagram)
                .writeIndividualModulesAsPlantUml(diagram)
                .writeModuleCanvases(canvas)
                .writeAggregatingDocument(diagram, canvas);
    }
}
