package io.quarkiverse.modulith.moments.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Objects;
import java.util.function.Consumer;

import jakarta.enterprise.event.Event;

import org.jboss.logging.Logger;

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
 * Publishes passage of time events through CDI, so that {@code @Observes} methods and
 * {@code @ApplicationModuleListener}s receive them.
 * <p>
 * The extension registers one scheduler job per unit of the configured {@link Granularity}. Each job publishes the
 * event of the period that has just ended: at 14:00, {@link HourHasPassed} for 13:00. At midnight, the daily job
 * publishes {@link DayHasPassed} and, when the day ends a week, month, quarter or year, {@link WeekHasPassed},
 * {@link MonthHasPassed}, {@link QuarterHasPassed} and {@link YearHasPassed}.
 * <p>
 * Each event is published on its own: when an observer of one event throws, the failure is logged with the event and
 * the remaining events of the same tick or shift are still published.
 */
public class Moments implements Now {

    private static final Logger LOG = Logger.getLogger(Moments.class);
    private static final MonthDay DEC_31ST = MonthDay.of(Month.DECEMBER, 31);

    private final Clock clock;
    private final Consumer<Object> events;
    private final MomentsProperties properties;

    private volatile Duration shift = Duration.ZERO;

    /**
     * Creates a new {@link Moments} for the given {@link Clock}, CDI {@link Event} and {@link MomentsProperties}.
     *
     * @param clock must not be {@literal null}
     * @param events must not be {@literal null}
     * @param properties must not be {@literal null}
     */
    public Moments(Clock clock, Event<Object> events, MomentsProperties properties) {
        this(clock, Objects.requireNonNull(events, "Event must not be null!")::fire, properties);
    }

    Moments(Clock clock, Consumer<Object> events, MomentsProperties properties) {
        this.clock = Objects.requireNonNull(clock, "Clock must not be null!");
        this.events = Objects.requireNonNull(events, "Events must not be null!");
        this.properties = Objects.requireNonNull(properties, "MomentsProperties must not be null!");
    }

    /**
     * Publishes the second that has just passed. Called by the scheduler every second.
     */
    void everySecond() {
        if (publishes(Granularity.SECONDS)) {
            publishSecond(now().minusSeconds(1));
        }
    }

    /**
     * Publishes the minute that has just passed. Called by the scheduler every minute.
     */
    void everyMinute() {
        if (publishes(Granularity.MINUTES)) {
            publishMinute(now().minusMinutes(1));
        }
    }

    /**
     * Publishes the hour that has just passed. Called by the scheduler every hour.
     */
    void everyHour() {
        if (publishes(Granularity.HOURS)) {
            publishHour(now().minusHours(1));
        }
    }

    /**
     * Publishes the day that has just passed and the week, month, quarter and year it ends. Called by the scheduler
     * every midnight.
     */
    void everyMidnight() {
        publishDay(now().toLocalDate().minusDays(1));
    }

    /**
     * Moves the time forward (or backward, for a negative duration) and publishes the events of every boundary of the
     * configured granularity the time passes on its way forward. Shifting backwards publishes nothing.
     *
     * @param duration must not be {@literal null}
     * @return this instance
     */
    synchronized Moments shiftBy(Duration duration) {
        Objects.requireNonNull(duration, "Duration must not be null!");
        Instant start = instant();
        shift = shift.plus(duration);
        if (duration.isNegative()) {
            return this;
        }
        ChronoUnit unit = properties.getGranularity().getUnit();
        LocalDateTime last = toLocal(start.plus(duration)).truncatedTo(unit);
        LocalDateTime boundary = toLocal(start).truncatedTo(unit).plus(1, unit);
        while (!boundary.isAfter(last)) {
            publishPassed(boundary);
            boundary = boundary.plus(1, unit);
        }
        return this;
    }

    /**
     * Drops any shift, so that the time is the one of the {@link Clock} again. Publishes nothing.
     *
     * @return this instance
     */
    synchronized Moments reset() {
        shift = Duration.ZERO;
        return this;
    }

    @Override
    public LocalDateTime now() {
        return toLocal(instant());
    }

    @Override
    public LocalDate today() {
        return now().toLocalDate();
    }

    @Override
    public Instant instant() {
        return clock.instant().plus(shift);
    }

    MomentsProperties getProperties() {
        return properties;
    }

    private LocalDateTime toLocal(Instant instant) {
        return LocalDateTime.ofInstant(instant, properties.getZoneId());
    }

    private boolean publishes(Granularity granularity) {
        return properties.getGranularity().includes(granularity);
    }

    private static boolean isStartOf(LocalDateTime boundary, ChronoUnit unit) {
        return boundary.truncatedTo(unit).equals(boundary);
    }

    private void publishPassed(LocalDateTime boundary) {
        if (publishes(Granularity.SECONDS)) {
            publishSecond(boundary.minusSeconds(1));
        }
        if (publishes(Granularity.MINUTES) && isStartOf(boundary, ChronoUnit.MINUTES)) {
            publishMinute(boundary.minusMinutes(1));
        }
        if (publishes(Granularity.HOURS) && isStartOf(boundary, ChronoUnit.HOURS)) {
            publishHour(boundary.minusHours(1));
        }
        if (isStartOf(boundary, ChronoUnit.DAYS)) {
            publishDay(boundary.toLocalDate().minusDays(1));
        }
    }

    /**
     * Publishes one event, so that an observer failing on it does not keep the coarser events of the same moment, a
     * {@link YearHasPassed} after a failed {@link DayHasPassed} for example, from being published.
     */
    private void publish(Object event) {
        try {
            events.accept(event);
        } catch (RuntimeException e) {
            LOG.errorf(e, "An observer of %s failed, publishing the next passage of time events", event);
        }
    }

    private void publishSecond(LocalDateTime time) {
        publish(SecondHasPassed.of(time.truncatedTo(ChronoUnit.SECONDS)));
    }

    private void publishMinute(LocalDateTime time) {
        publish(MinuteHasPassed.of(time.truncatedTo(ChronoUnit.MINUTES)));
    }

    private void publishHour(LocalDateTime time) {
        publish(HourHasPassed.of(time.truncatedTo(ChronoUnit.HOURS)));
    }

    private void publishDay(LocalDate date) {
        publish(DayHasPassed.of(date));

        WeekFields weekFields = WeekFields.of(properties.getLocale());
        TemporalField weekOfYear = weekFields.weekOfWeekBasedYear();
        int week = date.get(weekOfYear);
        if (date.plusDays(1).get(weekOfYear) != week) {
            Year weekBasedYear = Year.of(date.get(weekFields.weekBasedYear()));
            publish(WeekHasPassed.of(weekBasedYear, week, properties.getLocale()));
        }

        if (date.getDayOfMonth() == date.lengthOfMonth()) {
            publish(MonthHasPassed.of(YearMonth.from(date)));
        }

        ShiftedQuarter quarter = properties.getShiftedQuarter(date);
        if (quarter.isLastDay(date)) {
            publish(QuarterHasPassed.of(fiscalYear(quarter, date), quarter));
        }

        if (MonthDay.from(date).equals(DEC_31ST)) {
            publish(YearHasPassed.of(Year.from(date)));
        }
    }

    /**
     * The calendar year the fiscal year of the quarter containing the date starts in. A quarter that ends in January
     * 2027 with fiscal years starting in February belongs to the fiscal year 2026.
     */
    private static Year fiscalYear(ShiftedQuarter quarter, LocalDate date) {
        Year year = Year.from(date);
        return quarter.getStartDate(year).isAfter(date) ? year.minusYears(1) : year;
    }
}
