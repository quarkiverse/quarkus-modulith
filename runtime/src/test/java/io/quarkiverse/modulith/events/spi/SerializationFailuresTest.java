package io.quarkiverse.modulith.events.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SerializationFailuresTest {

    @ParameterizedTest
    @MethodSource("failures")
    void recognizesSqlState40001AnywhereInTheCauses(Throwable failure, boolean expected) {
        // arrange
        Throwable candidate = failure;

        // act
        boolean result = SerializationFailures.isSerializationFailure(candidate);

        // assert
        assertThat(result).isEqualTo(expected);
    }

    static List<Object[]> failures() {
        return List.of(new Object[] { new SQLException("serialize", "40001"), true },
                new Object[] { new IllegalStateException(new RuntimeException(new SQLException("x", "40001"))), true },
                new Object[] { new SQLException("duplicate", "23505"), false },
                new Object[] { new SQLException("no state"), false },
                new Object[] { new IllegalStateException("plain"), false });
    }
}
