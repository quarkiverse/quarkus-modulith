package io.quarkiverse.modulith.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExternalizationApiTest {

    @Test
    void hasResubmissionDefaults() {
        // act
        ResubmissionOptions defaults = ResubmissionOptions.defaults();

        // assert
        assertThat(defaults.getMaxInFlight()).isEqualTo(Integer.MAX_VALUE);
        assertThat(defaults.getBatchSize()).isEqualTo(100);
        assertThat(defaults.getMinAge()).isZero();
        assertThat(defaults.getFilter().test(null)).isTrue();
    }

    @Test
    void copiesResubmissionOptionsWithoutChangingTheOriginal() {
        // arrange
        ResubmissionOptions defaults = ResubmissionOptions.defaults();

        // act
        ResubmissionOptions custom = defaults.withMaxInFlight(5).withBatchSize(10).withMinAge(Duration.ofMinutes(1))
                .withFilter(publication -> false);

        // assert
        assertThat(custom.getMaxInFlight()).isEqualTo(5);
        assertThat(custom.getBatchSize()).isEqualTo(10);
        assertThat(custom.getMinAge()).isEqualTo(Duration.ofMinutes(1));
        assertThat(custom.getFilter().test(null)).isFalse();
        assertThat(defaults.getMaxInFlight()).isEqualTo(Integer.MAX_VALUE);
        assertThat(defaults.getBatchSize()).isEqualTo(100);
        assertThat(defaults.getMinAge()).isZero();
        assertThat(defaults.getFilter().test(null)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidOptions")
    void rejectsInvalidResubmissionOptions(UnaryOperator<ResubmissionOptions> change, String message) {
        // arrange
        ResubmissionOptions defaults = ResubmissionOptions.defaults();

        // act & assert
        assertThatThrownBy(() -> change.apply(defaults)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(message);
    }

    static List<Arguments> invalidOptions() {
        return List.of(
                Arguments.of((UnaryOperator<ResubmissionOptions>) options -> options.withMaxInFlight(0),
                        "maxInFlight must be at least 1: 0"),
                Arguments.of((UnaryOperator<ResubmissionOptions>) options -> options.withBatchSize(-1),
                        "batchSize must be at least 1: -1"),
                Arguments.of((UnaryOperator<ResubmissionOptions>) options -> options.withMinAge(Duration.ofSeconds(-1)),
                        "minAge must not be negative: PT-1S"));
    }
}
