package io.quarkiverse.modulith.events.mongodb.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BsonDatesTest {

    static Stream<Arguments> millis() {
        return Stream.of(Arguments.of(Instant.parse("2026-01-01T10:00:00Z"), 1767261600000L),
                Arguments.of(Instant.MAX, Long.MAX_VALUE),
                Arguments.of(Instant.MIN, Long.MIN_VALUE));
    }

    @ParameterizedTest
    @MethodSource
    void millis(Instant instant, long expected) {
        // act
        long millis = MongoDbEventPublicationRepository.millis(instant);

        // assert
        assertThat(millis).isEqualTo(expected);
    }
}
