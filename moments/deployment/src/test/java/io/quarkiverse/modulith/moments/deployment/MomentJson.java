package io.quarkiverse.modulith.moments.deployment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.Locale;
import java.util.function.Supplier;

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

/**
 * Every moment type with its JSON form, as the application's mapper of this Quarkus line writes it. Tests look the
 * constants up by name inside the application, so the moments are instances of the application's classes.
 */
enum MomentJson {

    SECOND(() -> SecondHasPassed.of(LocalDateTime.of(2026, 11, 16, 13, 0, 5)), "{\"time\":\"2026-11-16T13:00:05\"}"),
    MINUTE(() -> MinuteHasPassed.of(LocalDateTime.of(2026, 11, 16, 13, 5)), "{\"time\":\"2026-11-16T13:05:00\"}"),
    HOUR(() -> HourHasPassed.of(LocalDateTime.of(2026, 11, 16, 13, 0)), "{\"time\":\"2026-11-16T13:00:00\"}"),
    DAY(() -> DayHasPassed.of(LocalDate.of(2026, 11, 16)), "{\"date\":\"2026-11-16\"}"),
    WEEK(() -> WeekHasPassed.of(Year.of(2026), 46, Locale.US),
            "{\"year\":2026,\"week\":46,\"locale\":\"en-US\",\"startDate\":\"2026-11-08\",\"endDate\":\"2026-11-14\"}"),
    MONTH(() -> MonthHasPassed.of(YearMonth.of(2026, 2)), "{\"month\":\"2026-02\"}"),
    QUARTER(() -> QuarterHasPassed.of(Year.of(2026), Quarter.Q1),
            "{\"year\":2026,\"quarter\":{\"quarter\":\"Q1\",\"startMonth\":1}}"),
    SHIFTED_QUARTER_HAS_PASSED(() -> QuarterHasPassed.of(Year.of(2026), ShiftedQuarter.of(Quarter.Q3, Month.FEBRUARY)),
            "{\"year\":2026,\"quarter\":{\"quarter\":\"Q3\",\"startMonth\":2}}"),
    YEAR(() -> YearHasPassed.of(2026), "{\"year\":2026}"),
    SHIFTED_QUARTER(() -> ShiftedQuarter.of(Quarter.Q4, Month.APRIL), "{\"quarter\":\"Q4\",\"startMonth\":4}");

    private final Supplier<Object> moment;
    private final String json;

    MomentJson(Supplier<Object> moment, String json) {
        this.moment = moment;
        this.json = json;
    }

    Object moment() {
        return moment.get();
    }

    String json() {
        return json;
    }
}
