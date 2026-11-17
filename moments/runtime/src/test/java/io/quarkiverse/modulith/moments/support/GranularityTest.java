package io.quarkiverse.modulith.moments.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.temporal.ChronoUnit;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GranularityTest {

    @ParameterizedTest
    @CsvSource({
            "SECONDS, SECONDS, true", "SECONDS, MINUTES, true", "SECONDS, HOURS, true", "SECONDS, DAYS, true",
            "MINUTES, SECONDS, false", "MINUTES, MINUTES, true", "MINUTES, HOURS, true", "MINUTES, DAYS, true",
            "HOURS, SECONDS, false", "HOURS, MINUTES, false", "HOURS, HOURS, true", "HOURS, DAYS, true",
            "DAYS, SECONDS, false", "DAYS, MINUTES, false", "DAYS, HOURS, false", "DAYS, DAYS, true" })
    void includesItselfAndCoarserGranularities(Granularity configured, Granularity other, boolean expected) {
        // act
        boolean included = configured.includes(other);

        // assert
        assertThat(included).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "SECONDS, SECONDS", "MINUTES, MINUTES", "HOURS, HOURS", "DAYS, DAYS" })
    void hasATimeUnit(Granularity granularity, ChronoUnit expected) {
        // act
        ChronoUnit unit = granularity.getUnit();

        // assert
        assertThat(unit).isEqualTo(expected);
    }
}
