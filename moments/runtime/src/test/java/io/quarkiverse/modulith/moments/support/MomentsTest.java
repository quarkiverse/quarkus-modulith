package io.quarkiverse.modulith.moments.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.enterprise.event.Event;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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
import io.quarkiverse.modulith.moments.WeekHasPassed;
import io.quarkiverse.modulith.moments.YearHasPassed;

class MomentsTest {

    static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    final List<Object> events = new CopyOnWriteArrayList<>();

    Moments moments(String instant, Granularity granularity, Locale locale) {
        MomentsProperties properties = MomentsProperties.defaults().withGranularity(granularity).withLocale(locale);
        return new Moments(new MutableClock(Instant.parse(instant)), events::add, properties);
    }

    static List<Object> hours(LocalDateTime first, int count) {
        return IntStream.range(0, count).mapToObj(i -> (Object) HourHasPassed.of(first.plusHours(i))).toList();
    }

    @Test
    void publishesEveryHourAndTheDayWhenShiftingByADay() {
        // arrange
        Moments moments = moments("2026-11-16T10:30:00Z", Granularity.HOURS, Locale.GERMANY);
        List<Object> expected = new ArrayList<>(hours(LocalDateTime.of(2026, 11, 16, 10, 0), 14));
        expected.add(DayHasPassed.of(LocalDate.of(2026, 11, 16)));
        expected.addAll(hours(LocalDateTime.of(2026, 11, 17, 0, 0), 10));

        // act
        moments.shiftBy(Duration.ofDays(1));

        // assert
        assertThat(events).containsExactlyElementsOf(expected);
        assertThat(moments.now()).isEqualTo(LocalDateTime.of(2026, 11, 17, 10, 30));
    }

    @Test
    void publishesOnlyTheDayWhenShiftingByADayWithDailyGranularity() {
        // arrange
        Moments moments = moments("2026-11-16T10:30:00Z", Granularity.DAYS, Locale.GERMANY);

        // act
        moments.shiftBy(Duration.ofDays(1));

        // assert
        assertThat(events).containsExactly(DayHasPassed.of(LocalDate.of(2026, 11, 16)));
    }

    @Test
    void publishesNothingWithDailyGranularityWhenMidnightIsNotPassed() {
        // arrange
        Moments moments = moments("2026-11-16T01:00:00Z", Granularity.DAYS, Locale.GERMANY);

        // act
        moments.shiftBy(Duration.ofHours(22));

        // assert
        assertThat(events).isEmpty();
        assertThat(moments.now()).isEqualTo(LocalDateTime.of(2026, 11, 16, 23, 0));
    }

    @Test
    void publishesTheMonthWhenShiftingAcrossItsEnd() {
        // arrange
        Moments moments = moments("2026-01-30T12:00:00Z", Granularity.DAYS, Locale.GERMANY);

        // act
        moments.shiftBy(Duration.ofDays(2));

        // assert
        assertThat(events).containsExactly(
                DayHasPassed.of(LocalDate.of(2026, 1, 30)),
                DayHasPassed.of(LocalDate.of(2026, 1, 31)),
                MonthHasPassed.of(YearMonth.of(2026, 1)));
    }

    @Test
    void publishesSecondsMinutesAndHoursWithSecondGranularity() {
        // arrange
        Moments moments = moments("2026-11-16T09:59:58.500Z", Granularity.SECONDS, Locale.GERMANY);

        // act
        moments.shiftBy(Duration.ofSeconds(2));

        // assert
        assertThat(events).containsExactly(
                SecondHasPassed.of(LocalDateTime.of(2026, 11, 16, 9, 59, 58)),
                SecondHasPassed.of(LocalDateTime.of(2026, 11, 16, 9, 59, 59)),
                MinuteHasPassed.of(LocalDateTime.of(2026, 11, 16, 9, 59)),
                HourHasPassed.of(LocalDateTime.of(2026, 11, 16, 9, 0)));
    }

    @Test
    void publishesMinutesHoursAndTheDayWithMinuteGranularity() {
        // arrange
        Moments moments = moments("2026-11-16T23:58:30Z", Granularity.MINUTES, Locale.GERMANY);

        // act
        moments.shiftBy(Duration.ofMinutes(2));

        // assert
        assertThat(events).containsExactly(
                MinuteHasPassed.of(LocalDateTime.of(2026, 11, 16, 23, 58)),
                MinuteHasPassed.of(LocalDateTime.of(2026, 11, 16, 23, 59)),
                HourHasPassed.of(LocalDateTime.of(2026, 11, 16, 23, 0)),
                DayHasPassed.of(LocalDate.of(2026, 11, 16)));
    }

    @Test
    void publishesNothingWhenShiftingBackwards() {
        // arrange
        Moments moments = moments("2026-11-16T10:30:00Z", Granularity.SECONDS, Locale.GERMANY);

        // act
        moments.shiftBy(Duration.ofDays(-1));

        // assert
        assertThat(events).isEmpty();
        assertThat(moments.now()).isEqualTo(LocalDateTime.of(2026, 11, 15, 10, 30));
    }

    @Test
    void publishesNothingWhenShiftingByNothing() {
        // arrange
        Moments moments = moments("2026-11-16T23:59:59.999Z", Granularity.SECONDS, Locale.GERMANY);

        // act
        moments.shiftBy(Duration.ZERO);

        // assert
        assertThat(events).isEmpty();
        assertThat(moments.now()).isEqualTo(LocalDateTime.of(2026, 11, 16, 23, 59, 59, 999_000_000));
    }

    static Stream<Arguments> weeks() {
        return Stream.of(
                arguments("2026-11-14T12:00:00Z", 1, Locale.US, List.of(
                        DayHasPassed.of(LocalDate.of(2026, 11, 14)),
                        WeekHasPassed.of(Year.of(2026), 46, Locale.US))),
                arguments("2026-11-14T12:00:00Z", 1, Locale.GERMANY, List.of(
                        DayHasPassed.of(LocalDate.of(2026, 11, 14)))),
                arguments("2026-11-14T12:00:00Z", 2, Locale.GERMANY, List.of(
                        DayHasPassed.of(LocalDate.of(2026, 11, 14)),
                        DayHasPassed.of(LocalDate.of(2026, 11, 15)),
                        WeekHasPassed.of(Year.of(2026), 46, Locale.GERMANY))),
                arguments("2027-01-02T12:00:00Z", 1, Locale.US, List.of(
                        DayHasPassed.of(LocalDate.of(2027, 1, 2)),
                        WeekHasPassed.of(Year.of(2027), 1, Locale.US))),
                arguments("2027-01-03T12:00:00Z", 1, Locale.GERMANY, List.of(
                        DayHasPassed.of(LocalDate.of(2027, 1, 3)),
                        WeekHasPassed.of(Year.of(2026), 53, Locale.GERMANY))));
    }

    @ParameterizedTest
    @MethodSource("weeks")
    void publishesTheWeekOnTheLastDayOfTheWeekOfTheLocale(String instant, int days, Locale locale,
            List<Object> expected) {
        // arrange
        Moments moments = moments(instant, Granularity.DAYS, locale);

        // act
        moments.shiftBy(Duration.ofDays(days));

        // assert
        assertThat(events).containsExactlyElementsOf(expected);
    }

    @Test
    void publishesDayMonthQuarterAndYearAtTheEndOfTheYear() {
        // arrange
        Moments moments = moments("2026-12-31T23:30:00Z", Granularity.HOURS, Locale.US);

        // act
        moments.shiftBy(Duration.ofHours(1));

        // assert
        assertThat(events).containsExactly(
                HourHasPassed.of(LocalDateTime.of(2026, 12, 31, 23, 0)),
                DayHasPassed.of(LocalDate.of(2026, 12, 31)),
                MonthHasPassed.of(YearMonth.of(2026, 12)),
                QuarterHasPassed.of(Year.of(2026), Quarter.Q4),
                YearHasPassed.of(2026));
    }

    static Stream<Arguments> failingObservers() {
        Consumer<Moments> midnight = Moments::everyMidnight;
        Consumer<Moments> shift = it -> it.shiftBy(Duration.ofHours(1));
        return Stream.of(
                arguments("2027-01-01T00:00:00.003Z", DayHasPassed.class, midnight, List.of(
                        MonthHasPassed.of(YearMonth.of(2026, 12)),
                        QuarterHasPassed.of(Year.of(2026), Quarter.Q4),
                        YearHasPassed.of(2026))),
                arguments("2026-12-31T23:30:00Z", HourHasPassed.class, shift, List.of(
                        DayHasPassed.of(LocalDate.of(2026, 12, 31)),
                        MonthHasPassed.of(YearMonth.of(2026, 12)),
                        QuarterHasPassed.of(Year.of(2026), Quarter.Q4),
                        YearHasPassed.of(2026))),
                arguments("2026-12-31T23:30:00Z", MonthHasPassed.class, shift, List.of(
                        HourHasPassed.of(LocalDateTime.of(2026, 12, 31, 23, 0)),
                        DayHasPassed.of(LocalDate.of(2026, 12, 31)),
                        QuarterHasPassed.of(Year.of(2026), Quarter.Q4),
                        YearHasPassed.of(2026))));
    }

    @ParameterizedTest
    @MethodSource("failingObservers")
    void publishesTheOtherEventsWhenAnObserverFails(String instant, Class<?> failing, Consumer<Moments> tick,
            List<Object> expected) {
        // arrange
        Consumer<Object> observer = event -> {
            if (failing.isInstance(event)) {
                throw new IllegalStateException("broken");
            }
            events.add(event);
        };
        MomentsProperties properties = MomentsProperties.defaults().withLocale(Locale.GERMANY);
        Moments moments = new Moments(new MutableClock(Instant.parse(instant)), observer, properties);

        // act
        tick.accept(moments);

        // assert
        assertThat(events).containsExactlyElementsOf(expected);
    }

    static Stream<Arguments> quarters() {
        return Stream.of(
                arguments(Month.JANUARY, "2026-03-31T12:00:00Z", List.of(QuarterHasPassed.of(Year.of(2026), Quarter.Q1))),
                arguments(Month.JANUARY, "2026-03-30T12:00:00Z", List.of()),
                arguments(Month.APRIL, "2026-03-31T12:00:00Z",
                        List.of(QuarterHasPassed.of(Year.of(2025), Quarter.Q4, Month.APRIL))),
                arguments(Month.APRIL, "2026-06-30T12:00:00Z",
                        List.of(QuarterHasPassed.of(Year.of(2026), Quarter.Q1, Month.APRIL))),
                arguments(Month.OCTOBER, "2026-09-30T12:00:00Z",
                        List.of(QuarterHasPassed.of(Year.of(2025), Quarter.Q4, Month.OCTOBER))),
                arguments(Month.OCTOBER, "2026-12-31T12:00:00Z",
                        List.of(QuarterHasPassed.of(Year.of(2026), Quarter.Q1, Month.OCTOBER))),
                arguments(Month.FEBRUARY, "2026-07-31T12:00:00Z",
                        List.of(QuarterHasPassed.of(Year.of(2026), Quarter.Q2, Month.FEBRUARY))),
                arguments(Month.FEBRUARY, "2027-01-31T12:00:00Z",
                        List.of(QuarterHasPassed.of(Year.of(2026), Quarter.Q4, Month.FEBRUARY))),
                arguments(Month.DECEMBER, "2026-02-28T12:00:00Z",
                        List.of(QuarterHasPassed.of(Year.of(2025), Quarter.Q1, Month.DECEMBER))),
                arguments(Month.DECEMBER, "2028-02-28T12:00:00Z", List.of()),
                arguments(Month.DECEMBER, "2028-02-29T12:00:00Z",
                        List.of(QuarterHasPassed.of(Year.of(2027), Quarter.Q1, Month.DECEMBER))));
    }

    @ParameterizedTest
    @MethodSource("quarters")
    void publishesTheQuarterOfTheFiscalYearOnItsLastDay(Month startMonth, String instant, List<Object> expected) {
        // arrange
        MomentsProperties properties = MomentsProperties.defaults().withGranularity(Granularity.DAYS)
                .withQuarterStartMonth(startMonth);
        Moments moments = new Moments(new MutableClock(Instant.parse(instant)), events::add, properties);

        // act
        moments.shiftBy(Duration.ofDays(1));

        // assert
        assertThat(events.stream().filter(QuarterHasPassed.class::isInstance).toList())
                .containsExactlyElementsOf(expected);
    }

    @Test
    void publishesTheQuarterOnceWhenShiftingAcrossIt() {
        // arrange
        Moments moments = moments("2026-04-01T08:00:00Z", Granularity.HOURS, Locale.GERMANY);

        // act
        moments.shiftBy(Duration.ofDays(92));

        // assert
        assertThat(events.stream().filter(QuarterHasPassed.class::isInstance).toList())
                .containsExactly(QuarterHasPassed.of(Year.of(2026), Quarter.Q2));
        assertThat(events.stream().filter(DayHasPassed.class::isInstance).count()).isEqualTo(92);
        assertThat(events.stream().filter(HourHasPassed.class::isInstance).count()).isEqualTo(92 * 24);
    }

    @Test
    void usesTheConfiguredZoneForTimesAndDates() {
        // arrange
        MomentsProperties properties = MomentsProperties.defaults().withZoneId(BERLIN).withLocale(Locale.GERMANY);
        Moments moments = new Moments(new MutableClock(Instant.parse("2026-11-16T22:30:00Z")), events::add,
                properties);

        // act
        moments.shiftBy(Duration.ofHours(1));

        // assert
        assertThat(events).containsExactly(
                HourHasPassed.of(LocalDateTime.of(2026, 11, 16, 23, 0)),
                DayHasPassed.of(LocalDate.of(2026, 11, 16)));
        assertThat(moments.now()).isEqualTo(LocalDateTime.of(2026, 11, 17, 0, 30));
        assertThat(moments.today()).isEqualTo(LocalDate.of(2026, 11, 17));
        assertThat(moments.instant()).isEqualTo(Instant.parse("2026-11-16T23:30:00Z"));
    }

    static Stream<Arguments> ticks() {
        Consumer<Moments> second = Moments::everySecond;
        Consumer<Moments> minute = Moments::everyMinute;
        Consumer<Moments> hour = Moments::everyHour;
        Consumer<Moments> midnight = Moments::everyMidnight;
        return Stream.of(
                arguments(Granularity.SECONDS, second, List.of(SecondHasPassed.of(LocalDateTime.of(2026, 11, 15, 23, 59, 59)))),
                arguments(Granularity.MINUTES, second, List.of()),
                arguments(Granularity.MINUTES, minute, List.of(MinuteHasPassed.of(LocalDateTime.of(2026, 11, 15, 23, 59)))),
                arguments(Granularity.HOURS, minute, List.of()),
                arguments(Granularity.HOURS, hour, List.of(HourHasPassed.of(LocalDateTime.of(2026, 11, 15, 23, 0)))),
                arguments(Granularity.DAYS, hour, List.of()),
                arguments(Granularity.DAYS, midnight, List.of(
                        DayHasPassed.of(LocalDate.of(2026, 11, 15)),
                        WeekHasPassed.of(Year.of(2026), 46, Locale.GERMANY))));
    }

    @ParameterizedTest
    @MethodSource("ticks")
    void publishesThePeriodThatHasJustPassedOnEachScheduledTick(Granularity granularity, Consumer<Moments> tick,
            List<Object> expected) {
        // arrange
        Moments moments = moments("2026-11-16T00:00:00.003Z", granularity, Locale.GERMANY);

        // act
        tick.accept(moments);

        // assert
        assertThat(events).containsExactlyElementsOf(expected);
    }

    @Test
    void addsTheShiftToTheClock() {
        // arrange
        MutableClock clock = new MutableClock(Instant.parse("2026-11-16T10:00:00Z"));
        Moments moments = new Moments(clock, events::add, MomentsProperties.defaults());
        moments.shiftBy(Duration.ofMinutes(90));

        // act
        clock.set(Instant.parse("2026-11-16T11:00:00Z"));

        // assert
        assertThat(moments.instant()).isEqualTo(Instant.parse("2026-11-16T12:30:00Z"));
        assertThat(moments.now()).isEqualTo(LocalDateTime.of(2026, 11, 16, 12, 30));
    }

    @Test
    void resettingDropsTheShiftAndPublishesNothing() {
        // arrange
        Moments moments = moments("2026-11-16T10:00:00Z", Granularity.HOURS, Locale.GERMANY);
        moments.shiftBy(Duration.ofMinutes(-30)).shiftBy(Duration.ofMinutes(-30));

        // act
        Moments reset = moments.reset();

        // assert
        assertThat(reset).isSameAs(moments);
        assertThat(moments.instant()).isEqualTo(Instant.parse("2026-11-16T10:00:00Z"));
        assertThat(events).isEmpty();
    }

    @Test
    void exposesItsProperties() {
        // arrange
        MomentsProperties properties = MomentsProperties.defaults().withZoneId(ZoneOffset.ofHours(2));

        // act
        Moments moments = new Moments(new MutableClock(Instant.EPOCH), events::add, properties);

        // assert
        assertThat(moments.getProperties()).isSameAs(properties);
    }

    static Stream<Arguments> nullValues() {
        MutableClock clock = new MutableClock(Instant.EPOCH);
        Consumer<Object> sink = event -> {
        };
        MomentsProperties properties = MomentsProperties.defaults();
        return Stream.of(
                arguments((ThrowingCallable) () -> new Moments(null, sink, properties), "Clock must not be null!"),
                arguments((ThrowingCallable) () -> new Moments(clock, (Consumer<Object>) null, properties),
                        "Events must not be null!"),
                arguments((ThrowingCallable) () -> new Moments(clock, (Event<Object>) null,
                        properties), "Event must not be null!"),
                arguments((ThrowingCallable) () -> new Moments(clock, sink, null), "MomentsProperties must not be null!"),
                arguments((ThrowingCallable) () -> new Moments(clock, sink, properties).shiftBy(null),
                        "Duration must not be null!"));
    }

    @ParameterizedTest
    @MethodSource("nullValues")
    void rejectsNullValues(ThrowingCallable call, String message) {
        // act
        Throwable thrown = catchThrowable(call);

        // assert
        assertThat(thrown).isExactlyInstanceOf(NullPointerException.class).hasMessage(message);
    }
}
