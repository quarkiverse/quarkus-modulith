package io.quarkiverse.modulith.moments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.Year;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ShiftedQuarterTest {

    static Stream<Arguments> boundaries() {
        return Stream.of(
                arguments(Quarter.Q1, Month.JANUARY, MonthDay.of(1, 1), MonthDay.of(3, 31)),
                arguments(Quarter.Q1, Month.FEBRUARY, MonthDay.of(2, 1), MonthDay.of(4, 30)),
                arguments(Quarter.Q2, Month.FEBRUARY, MonthDay.of(5, 1), MonthDay.of(7, 31)),
                arguments(Quarter.Q3, Month.FEBRUARY, MonthDay.of(8, 1), MonthDay.of(10, 31)),
                arguments(Quarter.Q4, Month.FEBRUARY, MonthDay.of(11, 1), MonthDay.of(1, 31)),
                arguments(Quarter.Q1, Month.DECEMBER, MonthDay.of(12, 1), MonthDay.of(2, 29)),
                arguments(Quarter.Q4, Month.APRIL, MonthDay.of(1, 1), MonthDay.of(3, 31)),
                arguments(Quarter.Q4, Month.OCTOBER, MonthDay.of(7, 1), MonthDay.of(9, 30)));
    }

    @ParameterizedTest
    @MethodSource("boundaries")
    void shiftsTheFirstAndLastDayByTheStartMonth(Quarter quarter, Month startMonth, MonthDay start, MonthDay end) {
        // arrange
        ShiftedQuarter shifted = ShiftedQuarter.of(quarter, startMonth);

        // act
        MonthDay first = shifted.getStart();
        MonthDay last = shifted.getEnd();

        // assert
        assertThat(first).isEqualTo(start);
        assertThat(last).isEqualTo(end);
    }

    static Stream<Arguments> containedDates() {
        return Stream.of(
                arguments(Quarter.Q1, Month.JANUARY, LocalDate.of(2026, 1, 1), true),
                arguments(Quarter.Q1, Month.JANUARY, LocalDate.of(2026, 3, 31), true),
                arguments(Quarter.Q1, Month.JANUARY, LocalDate.of(2026, 4, 1), false),
                arguments(Quarter.Q1, Month.JANUARY, LocalDate.of(2025, 12, 31), false),
                arguments(Quarter.Q4, Month.FEBRUARY, LocalDate.of(2026, 10, 31), false),
                arguments(Quarter.Q4, Month.FEBRUARY, LocalDate.of(2026, 11, 1), true),
                arguments(Quarter.Q4, Month.FEBRUARY, LocalDate.of(2026, 12, 31), true),
                arguments(Quarter.Q4, Month.FEBRUARY, LocalDate.of(2027, 1, 1), true),
                arguments(Quarter.Q4, Month.FEBRUARY, LocalDate.of(2027, 1, 31), true),
                arguments(Quarter.Q4, Month.FEBRUARY, LocalDate.of(2027, 2, 1), false),
                arguments(Quarter.Q2, Month.FEBRUARY, LocalDate.of(2026, 7, 31), true));
    }

    @ParameterizedTest
    @MethodSource("containedDates")
    void containsTheDaysBetweenTheFirstAndLastDay(Quarter quarter, Month startMonth, LocalDate date,
            boolean expected) {
        // arrange
        ShiftedQuarter shifted = ShiftedQuarter.of(quarter, startMonth);

        // act
        boolean contained = shifted.contains(date);

        // assert
        assertThat(contained).isEqualTo(expected);
    }

    static Stream<Arguments> lastDays() {
        return Stream.of(
                arguments(Quarter.Q1, Month.JANUARY, LocalDate.of(2026, 3, 31), true),
                arguments(Quarter.Q1, Month.JANUARY, LocalDate.of(2026, 3, 30), false),
                arguments(Quarter.Q1, Month.JANUARY, LocalDate.of(2026, 4, 30), false),
                arguments(Quarter.Q2, Month.FEBRUARY, LocalDate.of(2026, 7, 30), false),
                arguments(Quarter.Q2, Month.FEBRUARY, LocalDate.of(2026, 7, 31), true),
                arguments(Quarter.Q1, Month.DECEMBER, LocalDate.of(2026, 2, 28), true),
                arguments(Quarter.Q1, Month.DECEMBER, LocalDate.of(2028, 2, 28), false),
                arguments(Quarter.Q1, Month.DECEMBER, LocalDate.of(2028, 2, 29), true));
    }

    @ParameterizedTest
    @MethodSource("lastDays")
    void recognizesTheLastDayIncludingLeapYears(Quarter quarter, Month startMonth, LocalDate date, boolean expected) {
        // arrange
        ShiftedQuarter shifted = ShiftedQuarter.of(quarter, startMonth);

        // act
        boolean lastDay = shifted.isLastDay(date);

        // assert
        assertThat(lastDay).isEqualTo(expected);
    }

    static Stream<Arguments> dates() {
        return Stream.of(
                arguments(Quarter.Q1, Month.JANUARY, 2022, LocalDate.of(2022, 1, 1), LocalDate.of(2022, 3, 31)),
                arguments(Quarter.Q1, Month.FEBRUARY, 2022, LocalDate.of(2022, 2, 1), LocalDate.of(2022, 4, 30)),
                arguments(Quarter.Q4, Month.FEBRUARY, 2022, LocalDate.of(2022, 11, 1), LocalDate.of(2023, 1, 31)),
                arguments(Quarter.Q4, Month.APRIL, 2026, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 3, 31)),
                arguments(Quarter.Q1, Month.DECEMBER, 2027, LocalDate.of(2027, 12, 1), LocalDate.of(2028, 2, 29)));
    }

    @ParameterizedTest
    @MethodSource("dates")
    void computesTheDatesInTheFiscalYearStartingInTheGivenYear(Quarter quarter, Month startMonth, int year,
            LocalDate start, LocalDate end) {
        // arrange
        ShiftedQuarter shifted = ShiftedQuarter.of(quarter, startMonth);

        // act
        LocalDate startDate = shifted.getStartDate(Year.of(year));
        LocalDate endDate = shifted.getEndDate(Year.of(year));

        // assert
        assertThat(startDate).isEqualTo(start);
        assertThat(endDate).isEqualTo(end);
    }

    @Test
    void keepsTheStartMonthForTheNextQuarter() {
        // arrange
        ShiftedQuarter shifted = ShiftedQuarter.of(Quarter.Q4, Month.APRIL);

        // act
        ShiftedQuarter next = shifted.next();

        // assert
        assertThat(next.getQuarter()).isEqualTo(Quarter.Q1);
        assertThat(next.getStartMonth()).isEqualTo(Month.APRIL);
    }

    @Test
    void startsInJanuaryByDefault() {
        // act
        ShiftedQuarter shifted = ShiftedQuarter.of(Quarter.Q3);

        // assert
        assertThat(shifted.getQuarter()).isEqualTo(Quarter.Q3);
        assertThat(shifted.getStartMonth()).isEqualTo(Month.JANUARY);
    }

    @Test
    void rejectsANullDate() {
        // arrange
        ShiftedQuarter shifted = ShiftedQuarter.of(Quarter.Q1);

        // act
        Throwable thrown = catchThrowable(() -> shifted.contains(null));

        // assert
        assertThat(thrown).isExactlyInstanceOf(NullPointerException.class).hasMessage("Reference date must not be null!");
    }

    @Test
    void rejectsANullYear() {
        // arrange
        ShiftedQuarter shifted = ShiftedQuarter.of(Quarter.Q1);

        // act
        Throwable thrown = catchThrowable(() -> shifted.getStartDate(null));

        // assert
        assertThat(thrown).isExactlyInstanceOf(NullPointerException.class).hasMessage("Year must not be null!");
    }
}
