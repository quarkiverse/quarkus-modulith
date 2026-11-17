package io.quarkiverse.modulith.moments;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

class PeriodDatesTest {

    @Test
    void returnsTheFirstAndLastDayOfAYear() {
        // arrange
        YearHasPassed event = YearHasPassed.of(2022);

        // act
        LocalDate start = event.getStartDate();
        LocalDate end = event.getEndDate();

        // assert
        assertThat(event.getYear()).isEqualTo(Year.of(2022));
        assertThat(start).isEqualTo(LocalDate.of(2022, Month.JANUARY, 1));
        assertThat(end).isEqualTo(LocalDate.of(2022, Month.DECEMBER, 31));
    }

    @Test
    void returnsTheFirstAndLastDayOfACalendarQuarter() {
        // arrange
        QuarterHasPassed event = QuarterHasPassed.of(Year.of(2022), Quarter.Q1);

        // act
        LocalDate start = event.getStartDate();
        LocalDate end = event.getEndDate();

        // assert
        assertThat(event.getYear()).isEqualTo(Year.of(2022));
        assertThat(event.getQuarter()).isEqualTo(ShiftedQuarter.of(Quarter.Q1, Month.JANUARY));
        assertThat(start).isEqualTo(LocalDate.of(2022, 1, 1));
        assertThat(end).isEqualTo(LocalDate.of(2022, 3, 31));
    }

    @Test
    void returnsTheFirstAndLastDayOfAShiftedQuarter() {
        // arrange
        QuarterHasPassed event = QuarterHasPassed.of(Year.of(2022), Quarter.Q1, Month.FEBRUARY);

        // act
        LocalDate start = event.getStartDate();
        LocalDate end = event.getEndDate();

        // assert
        assertThat(event.getQuarter()).isEqualTo(ShiftedQuarter.of(Quarter.Q1, Month.FEBRUARY));
        assertThat(start).isEqualTo(LocalDate.of(2022, 2, 1));
        assertThat(end).isEqualTo(LocalDate.of(2022, 4, 30));
    }

    @Test
    void exposesThePassedTimes() {
        // arrange
        LocalDateTime time = LocalDateTime.of(2026, 11, 16, 13, 0);

        // act
        LocalDateTime second = SecondHasPassed.of(time).getTime();
        LocalDateTime minute = MinuteHasPassed.of(time).getTime();
        LocalDateTime hour = HourHasPassed.of(time).getTime();
        LocalDate day = DayHasPassed.of(time.toLocalDate()).getDate();
        YearMonth month = MonthHasPassed.of(YearMonth.of(2026, 11)).getMonth();

        // assert
        assertThat(second).isEqualTo(LocalDateTime.of(2026, 11, 16, 13, 0));
        assertThat(minute).isEqualTo(LocalDateTime.of(2026, 11, 16, 13, 0));
        assertThat(hour).isEqualTo(LocalDateTime.of(2026, 11, 16, 13, 0));
        assertThat(day).isEqualTo(LocalDate.of(2026, 11, 16));
        assertThat(month).isEqualTo(YearMonth.of(2026, 11));
    }
}
