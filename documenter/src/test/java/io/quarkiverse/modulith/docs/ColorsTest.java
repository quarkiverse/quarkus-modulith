package io.quarkiverse.modulith.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ColorsTest {

    @ParameterizedTest
    @CsvSource({
            "#FF0000, #ff0000, #b20000",
            "SteelBlue, #4682b4, #315b7d",
            "white, #ffffff, #b2b2b2",
            "#000000, #000000, #000000"
    })
    void convertsToHexAndDarkens(String color, String hex, String darker) {
        // act
        String actualHex = Colors.toHex(color);
        String actualDarker = Colors.darker(actualHex);

        // assert
        assertThat(actualHex).isEqualTo(hex);
        assertThat(actualDarker).isEqualTo(darker);
    }

    @ParameterizedTest
    @ValueSource(strings = { "#ff00", "notacolor", "#gg0000" })
    void rejectsUnknownColors(String color) {
        // act
        Throwable thrown = catchThrowable(() -> Colors.toHex(color));

        // assert
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(color + " is not a valid hex colour code or HTML colour name.");
    }
}
