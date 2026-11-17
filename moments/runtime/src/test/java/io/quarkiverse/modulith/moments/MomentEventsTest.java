package io.quarkiverse.modulith.moments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.Locale;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MomentEventsTest {

    static final LocalDateTime TIME = LocalDateTime.of(2026, 3, 31, 23, 0);

    static Stream<Arguments> values() {
        return Stream.of(
                arguments(SecondHasPassed.of(TIME), SecondHasPassed.of(TIME), SecondHasPassed.of(TIME.plusSeconds(1)),
                        "SecondHasPassed[2026-03-31T23:00]"),
                arguments(MinuteHasPassed.of(TIME), MinuteHasPassed.of(TIME), MinuteHasPassed.of(TIME.plusMinutes(1)),
                        "MinuteHasPassed[2026-03-31T23:00]"),
                arguments(HourHasPassed.of(TIME), HourHasPassed.of(TIME), HourHasPassed.of(TIME.plusHours(1)),
                        "HourHasPassed[2026-03-31T23:00]"),
                arguments(DayHasPassed.of(TIME.toLocalDate()), DayHasPassed.of(LocalDate.of(2026, 3, 31)),
                        DayHasPassed.of(LocalDate.of(2026, 4, 1)), "DayHasPassed[2026-03-31]"),
                arguments(WeekHasPassed.of(Year.of(2026), 14, Locale.GERMANY),
                        WeekHasPassed.of(Year.of(2026), 14, Locale.GERMANY),
                        WeekHasPassed.of(Year.of(2026), 15, Locale.GERMANY), "WeekHasPassed[2026-W14, de-DE]"),
                arguments(WeekHasPassed.of(Year.of(2026), 14, Locale.GERMANY),
                        WeekHasPassed.of(Year.of(2026), 14, Locale.GERMANY),
                        WeekHasPassed.of(Year.of(2025), 14, Locale.GERMANY), "WeekHasPassed[2026-W14, de-DE]"),
                arguments(WeekHasPassed.of(Year.of(2026), 14, Locale.US), WeekHasPassed.of(Year.of(2026), 14, Locale.US),
                        WeekHasPassed.of(Year.of(2026), 14, Locale.GERMANY), "WeekHasPassed[2026-W14, en-US]"),
                arguments(MonthHasPassed.of(YearMonth.of(2026, 3)), MonthHasPassed.of(YearMonth.of(2026, 3)),
                        MonthHasPassed.of(YearMonth.of(2026, 4)), "MonthHasPassed[2026-03]"),
                arguments(QuarterHasPassed.of(Year.of(2026), Quarter.Q1), QuarterHasPassed.of(Year.of(2026), Quarter.Q1),
                        QuarterHasPassed.of(Year.of(2026), Quarter.Q2), "QuarterHasPassed[2026 Q1/JANUARY]"),
                arguments(QuarterHasPassed.of(Year.of(2026), Quarter.Q1), QuarterHasPassed.of(Year.of(2026), Quarter.Q1),
                        QuarterHasPassed.of(Year.of(2027), Quarter.Q1), "QuarterHasPassed[2026 Q1/JANUARY]"),
                arguments(YearHasPassed.of(2026), YearHasPassed.of(Year.of(2026)), YearHasPassed.of(2027),
                        "YearHasPassed[2026]"),
                arguments(ShiftedQuarter.of(Quarter.Q4, Month.APRIL), ShiftedQuarter.of(Quarter.Q4, Month.APRIL),
                        ShiftedQuarter.of(Quarter.Q3, Month.APRIL), "Q4/APRIL"),
                arguments(ShiftedQuarter.of(Quarter.Q4, Month.APRIL), ShiftedQuarter.of(Quarter.Q4, Month.APRIL),
                        ShiftedQuarter.of(Quarter.Q4, Month.OCTOBER), "Q4/APRIL"));
    }

    @ParameterizedTest
    @MethodSource("values")
    void equalsAnInstanceWithTheSameValues(Object value, Object same, Object other, String text) {
        // act
        boolean equal = value.equals(same);

        // assert
        assertThat(equal).isTrue();
        assertThat(value.hashCode()).isEqualTo(same.hashCode());
    }

    @ParameterizedTest
    @MethodSource("values")
    void differsFromAnInstanceWithOtherValues(Object value, Object same, Object other, String text) {
        // act
        boolean equal = value.equals(other);

        // assert
        assertThat(equal).isFalse();
    }

    @ParameterizedTest
    @MethodSource("values")
    void equalsItself(Object value, Object same, Object other, String text) {
        // act
        boolean equal = value.equals(value);

        // assert
        assertThat(equal).isTrue();
    }

    @ParameterizedTest
    @MethodSource("values")
    void differsFromOtherTypesAndNull(Object value, Object same, Object other, String text) {
        // act
        boolean equalToText = value.equals(text);
        boolean equalToNull = value.equals(null);

        // assert
        assertThat(equalToText).isFalse();
        assertThat(equalToNull).isFalse();
    }

    @ParameterizedTest
    @MethodSource("values")
    void describesItself(Object value, Object same, Object other, String text) {
        // act
        String description = value.toString();

        // assert
        assertThat(description).isEqualTo(text);
    }

    static Stream<Arguments> nullValues() {
        return Stream.of(
                arguments((ThrowingCallable) () -> SecondHasPassed.of(null), "LocalDateTime must not be null!"),
                arguments((ThrowingCallable) () -> MinuteHasPassed.of(null), "LocalDateTime must not be null!"),
                arguments((ThrowingCallable) () -> HourHasPassed.of(null), "LocalDateTime must not be null!"),
                arguments((ThrowingCallable) () -> DayHasPassed.of(null), "LocalDate must not be null!"),
                arguments((ThrowingCallable) () -> WeekHasPassed.of(null, 1, Locale.US), "Year must not be null!"),
                arguments((ThrowingCallable) () -> WeekHasPassed.of(Year.of(2026), 1, null),
                        "Locale must not be null!"),
                arguments((ThrowingCallable) () -> MonthHasPassed.of(null), "YearMonth must not be null!"),
                arguments((ThrowingCallable) () -> QuarterHasPassed.of(null, ShiftedQuarter.of(Quarter.Q1)),
                        "Year must not be null!"),
                arguments((ThrowingCallable) () -> QuarterHasPassed.of(Year.of(2026), (ShiftedQuarter) null),
                        "ShiftedQuarter must not be null!"),
                arguments((ThrowingCallable) () -> YearHasPassed.of(null), "Year must not be null!"),
                arguments((ThrowingCallable) () -> ShiftedQuarter.of(null, Month.APRIL), "Quarter must not be null!"),
                arguments((ThrowingCallable) () -> ShiftedQuarter.of(Quarter.Q1, null),
                        "Start Month must not be null!"));
    }

    @ParameterizedTest
    @MethodSource("nullValues")
    void rejectsNullValues(ThrowingCallable creation, String message) {
        // act
        Throwable thrown = catchThrowable(creation);

        // assert
        assertThat(thrown).isExactlyInstanceOf(NullPointerException.class).hasMessage(message);
    }
}
