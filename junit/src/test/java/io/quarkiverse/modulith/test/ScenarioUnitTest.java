package io.quarkiverse.modulith.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ScenarioUnitTest {

    static Stream<Arguments> states() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of(false, false),
                Arguments.of(true, true),
                Arguments.of(List.of(), false),
                Arguments.of(List.of(1), true),
                Arguments.of(Optional.empty(), false),
                Arguments.of(Optional.of(1), true),
                Arguments.of("value", true));
    }

    @ParameterizedTest
    @MethodSource("states")
    void acceptsChangedStatesByDefault(Object state, boolean changed) {
        // act
        boolean result = Scenario.isChanged(state);

        // assert
        assertThat(result).isEqualTo(changed);
        assertThat(Scenario.defaultAcceptance().test(state)).isEqualTo(changed);
    }
}
