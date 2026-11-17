package io.quarkiverse.modulith.core.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Deflater;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ClassLocations;
import io.quarkiverse.modulith.core.ModelData;
import io.quarkiverse.modulith.core.ModelData.ModuleData;
import io.quarkiverse.modulith.core.ModelData.NamedInterfaceData;
import io.quarkiverse.modulith.core.ModelData.ViolationData;

class ModelCodecTest {

    static final ModelData SMALL = new ModelData("Shop", "com.acme", List.of(), List.of(), "flowchart");

    @ParameterizedTest
    @ValueSource(strings = { "io.quarkiverse.modulith.core.fixtures.shop", "io.quarkiverse.modulith.core.fixtures.rules",
            "io.quarkiverse.modulith.core.fixtures.kinds", "io.quarkiverse.modulith.core.par.beans",
            "io.quarkiverse.modulith.core.par.cycle", "io.quarkiverse.modulith.core.par.jmolecules" })
    void restoresTheModelOfAnApplication(String basePackage) {
        // arrange
        ModelData data = ApplicationModules.of(basePackage, ClassLocations.TESTS).getModelData();

        // act
        ModelData restored = ModelCodec.decode(ModelCodec.encode(data));

        // assert
        assertThat(restored).isEqualTo(data);
    }

    @Test
    void restoresStringsOfAnyLengthAndCharacter() {
        // arrange
        String mermaid = "flowchart LR\n" + "módulo → 模块 ".repeat(5_000);
        ModuleData module = new ModuleData("pedidos", "Pedidões", "com.acme.pedidos", true, false, List.of(),
                List.of(new NamedInterfaceData("<<UNNAMED>>", List.of(), List.of())), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        ModelData data = new ModelData("Loja", "com.acme", List.of(module),
                List.of(new ViolationData("CYCLE", "pedidos", "pedidos -> pedidos", "", "ciclo")), mermaid);

        // act
        byte[] bytes = ModelCodec.encode(data);

        // assert
        assertThat(ModelCodec.decode(bytes)).isEqualTo(data);
    }

    static byte[] compressed(byte[] encoded) {
        Deflater deflater = new Deflater();
        deflater.setInput(encoded);
        deflater.finish();
        byte[] buffer = new byte[encoded.length + 64];
        int size = deflater.deflate(buffer);
        deflater.end();
        return ByteBuffer.allocate(Integer.BYTES + size).putInt(encoded.length).put(buffer, 0, size).array();
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 1 })
    void rejectsModelsWhoseDeclaredLengthIsWrong(int difference) {
        // arrange
        byte[] bytes = ModelCodec.encode(SMALL);
        int length = ByteBuffer.wrap(bytes).getInt();
        ByteBuffer.wrap(bytes).putInt(length + difference);

        // act & assert
        assertThatThrownBy(() -> ModelCodec.decode(bytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("The model holds ");
    }

    @Test
    void rejectsModelsWhoseCompressedPayloadIsTruncated() {
        // arrange
        byte[] bytes = ModelCodec.encode(SMALL);
        byte[] truncated = Arrays.copyOf(bytes, bytes.length - 4);

        // act & assert
        assertThatThrownBy(() -> ModelCodec.decode(truncated))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("The model holds ");
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, Integer.MIN_VALUE, ModelCodec.MAX_LENGTH + 1, Integer.MAX_VALUE })
    void rejectsModelsDeclaringAnImpossibleLength(int length) {
        // arrange
        byte[] bytes = ByteBuffer.allocate(Integer.BYTES + 1).putInt(length).array();

        // act & assert
        assertThatThrownBy(() -> ModelCodec.decode(bytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The model declares " + length + " bytes, expected 0 to " + ModelCodec.MAX_LENGTH);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 3 })
    void rejectsBytesTooShortForTheLength(int size) {
        // arrange
        byte[] bytes = new byte[size];

        // act & assert
        assertThatThrownBy(() -> ModelCodec.decode(bytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The model holds no length");
    }

    @Test
    void rejectsAnEncodingThatEndsTooEarly() {
        // arrange
        byte[] encoded = new ModelEncoder().model(SMALL).toByteArray();
        byte[] bytes = compressed(Arrays.copyOf(encoded, encoded.length / 2));

        // act & assert
        assertThatThrownBy(() -> ModelCodec.decode(bytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The model is corrupt")
                .hasCauseInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void rejectsBytesThatAreNotAModel() {
        // arrange
        byte[] bytes = { 0, 0, 0, 8, 1, 2, 3, 4, 5, 6, 7, 8 };

        // act & assert
        assertThatThrownBy(() -> ModelCodec.decode(bytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The model is corrupt");
    }
}
