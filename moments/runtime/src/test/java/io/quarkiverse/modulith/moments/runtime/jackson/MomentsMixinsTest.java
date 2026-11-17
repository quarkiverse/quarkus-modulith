package io.quarkiverse.modulith.moments.runtime.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkiverse.modulith.moments.DayHasPassed;
import io.quarkiverse.modulith.moments.HourHasPassed;
import io.quarkiverse.modulith.moments.MinuteHasPassed;
import io.quarkiverse.modulith.moments.MonthHasPassed;
import io.quarkiverse.modulith.moments.Quarter;
import io.quarkiverse.modulith.moments.QuarterHasPassed;
import io.quarkiverse.modulith.moments.SecondHasPassed;
import io.quarkiverse.modulith.moments.ShiftedQuarter;
import io.quarkiverse.modulith.moments.WeekHasPassed;
import io.quarkiverse.modulith.moments.YearHasPassed;

class MomentsMixinsTest {

    static final LocalDateTime TIME = LocalDateTime.of(2026, 11, 16, 13, 0);

    static Stream<Arguments> creators() {
        return Stream.of(
                arguments((Supplier<Object>) () -> SecondHasPassedMixin.of(TIME), SecondHasPassed.of(TIME)),
                arguments((Supplier<Object>) () -> MinuteHasPassedMixin.of(TIME), MinuteHasPassed.of(TIME)),
                arguments((Supplier<Object>) () -> HourHasPassedMixin.of(TIME), HourHasPassed.of(TIME)),
                arguments((Supplier<Object>) () -> DayHasPassedMixin.of(LocalDate.of(2026, 11, 16)),
                        DayHasPassed.of(LocalDate.of(2026, 11, 16))),
                arguments((Supplier<Object>) () -> WeekHasPassedMixin.of(Year.of(2026), 46, Locale.US),
                        WeekHasPassed.of(Year.of(2026), 46, Locale.US)),
                arguments((Supplier<Object>) () -> MonthHasPassedMixin.of(YearMonth.of(2026, 11)),
                        MonthHasPassed.of(YearMonth.of(2026, 11))),
                arguments((Supplier<Object>) () -> QuarterHasPassedMixin.of(Year.of(2026),
                        ShiftedQuarter.of(Quarter.Q2, Month.APRIL)),
                        QuarterHasPassed.of(Year.of(2026), Quarter.Q2, Month.APRIL)),
                arguments((Supplier<Object>) () -> YearHasPassedMixin.of(Year.of(2026)), YearHasPassed.of(2026)),
                arguments((Supplier<Object>) () -> ShiftedQuarterMixin.of(Quarter.Q2, Month.APRIL),
                        ShiftedQuarter.of(Quarter.Q2, Month.APRIL)));
    }

    @ParameterizedTest
    @MethodSource("creators")
    void creatorsDelegateToTheFactoryMethods(Supplier<Object> creator, Object expected) {
        // act
        Object created = creator.get();

        // assert
        assertThat(created).isEqualTo(expected);
    }

    @Test
    void coversEveryMomentType() {
        // act
        var targets = MomentsMixins.MIXINS.keySet();

        // assert
        assertThat(targets).containsExactlyInAnyOrder(SecondHasPassed.class, MinuteHasPassed.class,
                HourHasPassed.class, DayHasPassed.class, WeekHasPassed.class, MonthHasPassed.class,
                QuarterHasPassed.class, YearHasPassed.class, ShiftedQuarter.class);
    }
}
