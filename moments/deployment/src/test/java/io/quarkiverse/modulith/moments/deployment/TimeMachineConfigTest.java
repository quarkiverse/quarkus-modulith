package io.quarkiverse.modulith.moments.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.moments.deployment.fixtures.Jobs;
import io.quarkiverse.modulith.moments.deployment.fixtures.TestClock;
import io.quarkiverse.modulith.moments.support.Moments;
import io.quarkiverse.modulith.moments.support.Now;
import io.quarkiverse.modulith.moments.support.TimeMachine;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusExtensionTest;

class TimeMachineConfigTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestClock.class, Jobs.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideConfigKey("quarkus.modulith.moments.enable-time-machine", "true")
            .overrideConfigKey("quarkus.modulith.moments.granularity", "minutes")
            .overrideConfigKey("quarkus.modulith.moments.zone-id", "Europe/Berlin")
            .overrideConfigKey("quarkus.modulith.moments.locale", "en-US")
            .overrideConfigKey("quarkus.modulith.moments.quarter-start-month", "April");

    @Inject
    TimeMachine timeMachine;

    @Inject
    Moments moments;

    @Inject
    Now now;

    @Inject
    TestClock clock;

    @Inject
    Scheduler scheduler;

    @BeforeEach
    void resetTime() {
        clock.set(TestClock.START);
        timeMachine.reset();
    }

    @Test
    void replacesMoments() {
        // act
        Class<?> type = moments.getClass();

        // assert
        assertThat(type).isEqualTo(TimeMachine.class);
        assertThat(moments).isSameAs(timeMachine);
        assertThat(now).isSameAs(timeMachine);
    }

    @Test
    void schedulesTheJobsOfTheGranularity() {
        // act
        var identities = Jobs.identities(scheduler);

        // assert
        assertThat(identities).containsExactly("modulith-moments-days", "modulith-moments-hours",
                "modulith-moments-minutes");
    }

    @Test
    void usesTheApplicationClockInTheConfiguredZone() {
        // act
        LocalDateTime local = now.now();

        // assert
        assertThat(now.instant()).isEqualTo(TestClock.START);
        assertThat(local).isEqualTo(LocalDateTime.of(2026, 3, 31, 23, 58, 30));
        assertThat(now.today()).isEqualTo(LocalDate.of(2026, 3, 31));
    }
}
