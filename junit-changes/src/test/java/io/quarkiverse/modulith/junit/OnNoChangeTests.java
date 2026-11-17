package io.quarkiverse.modulith.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OnNoChangeTests {

    @ParameterizedTest
    @CsvSource({
            "execute-all, EXECUTE_ALL",
            "EXECUTE-ALL, EXECUTE_ALL",
            "skip-all, SKIP_ALL",
            "Skip-All, SKIP_ALL",
            "execute-none, SKIP_ALL"
    })
    void parsesConfiguredValue(String value, OnNoChange expected) {
        // arrange
        var configured = Optional.of(value);

        // act
        var result = OnNoChange.fromConfig(configured);

        // assert
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void executesAllTestsByDefault() {
        // arrange
        Optional<String> configured = Optional.empty();

        // act
        var result = OnNoChange.fromConfig(configured);

        // assert
        assertThat(result).isEqualTo(OnNoChange.EXECUTE_ALL);
    }

    @Test
    void rejectsInvalidValue() {
        // arrange
        var configured = Optional.of("1");

        // act
        var thrown = catchThrowable(() -> OnNoChange.fromConfig(configured));

        // assert
        assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid on-no-changes value '1'! Needs to be any of [execute-all, skip-all, execute-none]!");
    }
}
