package io.quarkiverse.modulith.runtime.endpoint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ModulithEndpointHandlerTest {

    @ParameterizedTest
    @CsvSource({ "0, 0", "1, 1", "16384, 1", "16385, 2", "100000, 7" })
    void splitsTheJsonIntoPartsThatFitAClassFileConstant(int length, int count) {
        // arrange
        String json = "€".repeat(length);

        // act
        List<String> parts = ModulithEndpointHandler.parts(json);

        // assert
        assertThat(parts).hasSize(count).allSatisfy(it -> assertThat(it.length()).isLessThanOrEqualTo(16_384));
        assertThat(String.join("", parts)).isEqualTo(json);
    }
}
