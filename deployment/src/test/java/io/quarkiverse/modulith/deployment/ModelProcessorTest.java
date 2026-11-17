package io.quarkiverse.modulith.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkiverse.modulith.core.ApplicationModulesFactory;
import io.quarkiverse.modulith.core.ModelData;
import io.quarkiverse.modulith.deployment.model.Fixtures;
import io.quarkiverse.modulith.deployment.moduletest.NestedModulesFactory;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class ModelProcessorTest {

    static final ModuleModelBuildItem NO_MODEL = new ModuleModelBuildItem(null, "graph");

    @Test
    void registersTheModulithTypeForReflection() {
        // arrange
        List<ReflectiveClassBuildItem> produced = new ArrayList<>();
        ModuleModelBuildItem model = new ModuleModelBuildItem(Fixtures.model(
                "io.quarkiverse.modulith.deployment.fixtures.valid", Optional.empty()), "");

        // act
        new ModelProcessor().registerMainClass(model, produced::add);

        // assert
        assertThat(produced).singleElement().satisfies(it -> assertThat(it.getClassNames())
                .containsExactly("io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication"));
    }

    @Test
    void registersNothingForAModelBuiltFromThePackage() {
        // arrange
        ModuleModelBuildItem model = new ModuleModelBuildItem(Fixtures.model(
                "io.quarkiverse.modulith.deployment.fixtures.cycle",
                Optional.of("io.quarkiverse.modulith.deployment.fixtures.cycle")), "");

        // act
        Optional<String> mainClass = ModelProcessor.mainClass(model);

        // assert
        assertThat(mainClass).isEmpty();
    }

    @Test
    void hasNoMainClassWithoutModel() {
        // act
        Optional<String> mainClass = ModelProcessor.mainClass(NO_MODEL);

        // assert
        assertThat(mainClass).isEmpty();
    }

    @Test
    void recordsTheDiagramWithoutModel() {
        // act
        String mermaid = NO_MODEL.getModelData().mermaid();

        // assert
        assertThat(mermaid).isEqualTo("graph");
        assertThat(NO_MODEL.getApplicationModules()).isEmpty();
        assertThat(NO_MODEL.model()).isEmpty();
        assertThat(NO_MODEL.getModel()).isNull();
    }

    @Test
    void failsOnAModulithTypeItCannotLoad() {
        // act
        Throwable thrown = catchThrowable(() -> ModelProcessor.load("com.acme.Missing", getClass().getClassLoader()));

        // assert
        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("Cannot load the @Modulith type"
                + " com.acme.Missing for the registered ApplicationModulesFactory");
    }

    @Test
    void usesNoFactoryWithoutModulithType(@TempDir Path services) throws IOException {
        // arrange
        Path registration = services.resolve("META-INF/services/" + ApplicationModulesFactory.class.getName());
        Files.createDirectories(registration.getParent());
        Files.writeString(registration, NestedModulesFactory.class.getName());

        // act
        Optional<ModelData> model;
        try (URLClassLoader loader = new URLClassLoader(new URL[] { services.toUri().toURL() },
                getClass().getClassLoader())) {
            model = ModelProcessor.registeredFactoryModel(Optional.empty(), loader);
        }

        // assert
        assertThat(model).isEmpty();
    }

    @Test
    void usesNoFactoryWithoutRegistration() {
        // act
        Optional<ModelData> model = ModelProcessor.registeredFactoryModel(
                Optional.of("io.quarkiverse.modulith.deployment.fixtures.valid.ShopApplication"),
                getClass().getClassLoader());

        // assert
        assertThat(model).isEmpty();
    }
}
