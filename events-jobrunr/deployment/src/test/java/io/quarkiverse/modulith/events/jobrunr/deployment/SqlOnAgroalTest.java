package io.quarkiverse.modulith.events.jobrunr.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SqlOnAgroalTest {

    @ParameterizedTest
    @CsvSource({ ", true, true", "sql, true, true", "SQL, true, true", "mem, true, false", "mongodb, true, false",
            ", false, false", "sql, false, false" })
    void storesTheJobsThroughAgroalWhenJobRunrUsesSql(String type, boolean agroal, boolean expected) {
        // act
        boolean sql = EventsJobrunrProcessor.sqlOnAgroal(Optional.ofNullable(type), agroal);

        // assert
        assertThat(sql).isEqualTo(expected);
    }
}
