package io.quarkiverse.modulith.moments.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import io.quarkiverse.modulith.moments.DayHasPassed;
import io.quarkiverse.modulith.moments.HourHasPassed;

class TimeMachineTest {

    final List<Object> events = new CopyOnWriteArrayList<>();

    final TimeMachine timeMachine = new TimeMachine(new MutableClock(Instant.parse("2026-11-16T22:15:00Z")),
            events::add, MomentsProperties.defaults().withLocale(Locale.GERMANY));

    @Test
    void shiftsTimeAndPublishesTheEventsOnTheWay() {
        // act
        TimeMachine shifted = timeMachine.shiftBy(Duration.ofHours(2));

        // assert
        assertThat(shifted).isSameAs(timeMachine);
        assertThat(timeMachine.now()).isEqualTo(LocalDateTime.of(2026, 11, 17, 0, 15));
        assertThat(events).containsExactly(
                HourHasPassed.of(LocalDateTime.of(2026, 11, 16, 22, 0)),
                HourHasPassed.of(LocalDateTime.of(2026, 11, 16, 23, 0)),
                DayHasPassed.of(LocalDate.of(2026, 11, 16)));
    }

    @Test
    void resetsTimeWithoutPublishing() {
        // arrange
        timeMachine.shiftBy(Duration.ofMinutes(30));
        events.clear();

        // act
        TimeMachine reset = timeMachine.reset();

        // assert
        assertThat(reset).isSameAs(timeMachine);
        assertThat(timeMachine.instant()).isEqualTo(Instant.parse("2026-11-16T22:15:00Z"));
        assertThat(events).isEmpty();
    }
}
