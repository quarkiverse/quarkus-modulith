package io.quarkiverse.modulith.moments.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.moments.DayHasPassed;
import io.quarkiverse.modulith.moments.HourHasPassed;
import io.quarkiverse.modulith.moments.MinuteHasPassed;
import io.quarkiverse.modulith.moments.MonthHasPassed;
import io.quarkiverse.modulith.moments.Quarter;
import io.quarkiverse.modulith.moments.QuarterHasPassed;
import io.quarkiverse.modulith.moments.deployment.fixtures.RecordedMoments;
import io.quarkiverse.modulith.moments.deployment.fixtures.TestClock;
import io.quarkiverse.modulith.moments.support.Now;
import io.quarkiverse.modulith.moments.support.TimeMachine;
import io.quarkus.test.QuarkusExtensionTest;

class TimeMachineEventsTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(RecordedMoments.class, TestClock.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideConfigKey("quarkus.scheduler.enabled", "false")
            .overrideConfigKey("quarkus.modulith.moments.enable-time-machine", "true")
            .overrideConfigKey("quarkus.modulith.moments.granularity", "minutes")
            .overrideConfigKey("quarkus.modulith.moments.zone-id", "Europe/Berlin")
            .overrideConfigKey("quarkus.modulith.moments.locale", "en-US")
            .overrideConfigKey("quarkus.modulith.moments.quarter-start-month", "April");

    @Inject
    TimeMachine timeMachine;

    @Inject
    Now now;

    @Inject
    RecordedMoments recorded;

    @Test
    void firesEveryEventOnTheWayThroughCdi() {
        // act
        timeMachine.shiftBy(Duration.ofMinutes(2));

        // assert
        assertThat(recorded.events()).containsExactly(
                MinuteHasPassed.of(LocalDateTime.of(2026, 3, 31, 23, 58)),
                MinuteHasPassed.of(LocalDateTime.of(2026, 3, 31, 23, 59)),
                HourHasPassed.of(LocalDateTime.of(2026, 3, 31, 23, 0)),
                DayHasPassed.of(LocalDate.of(2026, 3, 31)),
                MonthHasPassed.of(YearMonth.of(2026, 3)),
                QuarterHasPassed.of(Year.of(2025), Quarter.Q4, Month.APRIL));
        assertThat(now.now()).isEqualTo(LocalDateTime.of(2026, 4, 1, 0, 0, 30));
    }
}
