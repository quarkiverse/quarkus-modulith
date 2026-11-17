package io.quarkiverse.modulith.moments.runtime.jackson;

import java.util.Map;

import io.quarkiverse.modulith.moments.DayHasPassed;
import io.quarkiverse.modulith.moments.HourHasPassed;
import io.quarkiverse.modulith.moments.MinuteHasPassed;
import io.quarkiverse.modulith.moments.MonthHasPassed;
import io.quarkiverse.modulith.moments.QuarterHasPassed;
import io.quarkiverse.modulith.moments.SecondHasPassed;
import io.quarkiverse.modulith.moments.ShiftedQuarter;
import io.quarkiverse.modulith.moments.WeekHasPassed;
import io.quarkiverse.modulith.moments.YearHasPassed;

/**
 * The Jackson mix-ins of the moment types, shared by the Jackson 2 and Jackson 3 modules. The annotations live in
 * {@code com.fasterxml.jackson.annotation}, which both Jackson lines use.
 */
public final class MomentsMixins {

    /**
     * Mix-in by target type.
     */
    public static final Map<Class<?>, Class<?>> MIXINS = Map.of(
            SecondHasPassed.class, SecondHasPassedMixin.class,
            MinuteHasPassed.class, MinuteHasPassedMixin.class,
            HourHasPassed.class, HourHasPassedMixin.class,
            DayHasPassed.class, DayHasPassedMixin.class,
            WeekHasPassed.class, WeekHasPassedMixin.class,
            MonthHasPassed.class, MonthHasPassedMixin.class,
            QuarterHasPassed.class, QuarterHasPassedMixin.class,
            YearHasPassed.class, YearHasPassedMixin.class,
            ShiftedQuarter.class, ShiftedQuarterMixin.class);

    private MomentsMixins() {
    }
}
