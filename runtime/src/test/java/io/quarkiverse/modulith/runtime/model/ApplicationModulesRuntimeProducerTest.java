package io.quarkiverse.modulith.runtime.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.core.ModelData;
import io.quarkiverse.modulith.core.data.ModelCodec;

class ApplicationModulesRuntimeProducerTest {

    @Test
    void readsTheModelTheBuildWrote() {
        // arrange
        ModelData data = new ModelData("Shop", "com.acme", List.of(), List.of(), "flowchart");
        ResourceLoader loader = new ResourceLoader(
                Map.of(ModelCodec.LOCATION, new ByteArrayInputStream(ModelCodec.encode(data))));

        // act
        byte[] bytes = ApplicationModulesRuntimeProducer.read(loader);

        // assert
        assertThat(ModelCodec.decode(bytes)).isEqualTo(data);
    }

    @Test
    void explainsAMissingModel() {
        // arrange
        ResourceLoader loader = new ResourceLoader(Map.of());

        // act & assert
        assertThatThrownBy(() -> ApplicationModulesRuntimeProducer.read(loader))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("META-INF/quarkus-modulith/model.bin is missing, the application was not built with the"
                        + " quarkus-modulith extension");
    }

    @Test
    void reportsAnUnreadableModel() {
        // arrange
        ResourceLoader loader = new ResourceLoader(Map.of(ModelCodec.LOCATION, new BrokenStream()));

        // act & assert
        assertThatThrownBy(() -> ApplicationModulesRuntimeProducer.read(loader))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessage("Could not read META-INF/quarkus-modulith/model.bin");
    }
}
