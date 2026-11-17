package io.quarkiverse.modulith.moments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.LocalDate;
import java.time.Year;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class WeekHasPassedTest {

    static Stream<Arguments> weeks() {
        return Stream.of(
                arguments(Locale.GERMANY, 2022, 7, LocalDate.of(2022, 2, 14), LocalDate.of(2022, 2, 20)),
                arguments(Locale.US, 2022, 8, LocalDate.of(2022, 2, 13), LocalDate.of(2022, 2, 19)),
                arguments(Locale.US, 2022, 53, LocalDate.of(2022, 12, 25), LocalDate.of(2022, 12, 31)),
                arguments(Locale.US, 2026, 1, LocalDate.of(2025, 12, 28), LocalDate.of(2026, 1, 3)),
                arguments(Locale.US, 2027, 1, LocalDate.of(2026, 12, 27), LocalDate.of(2027, 1, 2)),
                arguments(Locale.GERMANY, 2026, 1, LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 4)),
                arguments(Locale.GERMANY, 2026, 53, LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 3)));
    }

    @ParameterizedTest
    @MethodSource("weeks")
    void computesTheFirstAndLastDayOfTheWeekOfTheLocale(Locale locale, int year, int week, LocalDate start,
            LocalDate end) {
        // arrange
        WeekHasPassed event = WeekHasPassed.of(Year.of(year), week, locale);

        // act
        LocalDate startDate = event.getStartDate();
        LocalDate endDate = event.getEndDate();

        // assert
        assertThat(startDate).isEqualTo(start);
        assertThat(endDate).isEqualTo(end);
    }

    @Test
    void exposesItsValues() {
        // arrange
        WeekHasPassed event = WeekHasPassed.of(Year.of(2026), 46, Locale.US);

        // act
        Year year = event.getYear();
        int week = event.getWeek();
        Locale locale = event.getLocale();

        // assert
        assertThat(year).isEqualTo(Year.of(2026));
        assertThat(week).isEqualTo(46);
        assertThat(locale).isEqualTo(Locale.US);
    }

    @Test
    void usesTheDefaultLocaleWhenNoneIsGiven() {
        // act
        WeekHasPassed event = WeekHasPassed.of(Year.of(2026), 46);

        // assert
        assertThat(event).isEqualTo(WeekHasPassed.of(Year.of(2026), 46, Locale.getDefault()));
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 53 })
    void acceptsTheFirstAndTheLastPossibleWeek(int week) {
        // act
        WeekHasPassed event = WeekHasPassed.of(Year.of(2026), week, Locale.GERMANY);

        // assert
        assertThat(event.getWeek()).isEqualTo(week);
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 0, 54 })
    void rejectsWeeksOutsideTheYear(int week) {
        // act
        Throwable thrown = catchThrowable(() -> WeekHasPassed.of(Year.of(2026), week, Locale.GERMANY));

        // assert
        assertThat(thrown).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Week must be between 1 and 53, got " + week + "!");
    }
}
