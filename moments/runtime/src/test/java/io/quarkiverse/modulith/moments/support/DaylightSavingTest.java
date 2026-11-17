package io.quarkiverse.modulith.moments.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.modulith.moments.HourHasPassed;

class DaylightSavingTest {

    private static final MomentsProperties BERLIN = MomentsProperties.defaults()
            .withZoneId(ZoneId.of("Europe/Berlin")).withLocale(Locale.GERMANY);

    private final List<Object> events = new CopyOnWriteArrayList<>();

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "2026-03-29T01:00:00Z|2026-03-29T02:00",
            "2026-03-29T02:00:00Z|2026-03-29T03:00",
            "2026-10-25T00:00:00Z|2026-10-25T01:00",
            "2026-10-25T01:00:00Z|2026-10-25T01:00",
            "2026-10-25T02:00:00Z|2026-10-25T02:00"
    })
    void hourlyJobsPublishTheLocalHourBeforeNow(Instant tick, LocalDateTime published) {
        // arrange
        Moments moments = new Moments(new MutableClock(tick), events::add, BERLIN);

        // act
        moments.everyHour();

        // assert
        assertThat(events).containsExactly(HourHasPassed.of(published));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "2026-03-29T00:30:00Z|2026-03-29T01:00,2026-03-29T02:00,2026-03-29T03:00",
            "2026-10-25T00:30:00Z|2026-10-25T02:00"
    })
    void timeMachineCountsLocalHoursAcrossTheTransition(Instant start, String published) {
        // arrange
        TimeMachine timeMachine = new TimeMachine(new MutableClock(start), events::add, BERLIN);

        // act
        timeMachine.shiftBy(Duration.ofHours(2));

        // assert
        assertThat(events).containsExactlyElementsOf(List.of(published.split(",")).stream()
                .map(LocalDateTime::parse).map(HourHasPassed::of).toList());
    }
}
