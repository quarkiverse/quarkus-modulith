package io.quarkiverse.modulith.moments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.Month;
import java.time.MonthDay;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class QuarterTest {

    static Stream<Arguments> quarters() {
        return Stream.of(
                arguments(Quarter.Q1, Quarter.Q2, MonthDay.of(Month.JANUARY, 1), MonthDay.of(Month.MARCH, 31)),
                arguments(Quarter.Q2, Quarter.Q3, MonthDay.of(Month.APRIL, 1), MonthDay.of(Month.JUNE, 30)),
                arguments(Quarter.Q3, Quarter.Q4, MonthDay.of(Month.JULY, 1), MonthDay.of(Month.SEPTEMBER, 30)),
                arguments(Quarter.Q4, Quarter.Q1, MonthDay.of(Month.OCTOBER, 1), MonthDay.of(Month.DECEMBER, 31)));
    }

    @ParameterizedTest
    @MethodSource("quarters")
    void followsThePreviousQuarter(Quarter quarter, Quarter next, MonthDay start, MonthDay end) {
        // act
        Quarter following = quarter.next();

        // assert
        assertThat(following).isEqualTo(next);
    }

    @ParameterizedTest
    @MethodSource("quarters")
    void spansThreeCalendarMonths(Quarter quarter, Quarter next, MonthDay start, MonthDay end) {
        // act
        MonthDay first = quarter.getStart();
        MonthDay last = quarter.getEnd();

        // assert
        assertThat(first).isEqualTo(start);
        assertThat(last).isEqualTo(end);
    }
}
