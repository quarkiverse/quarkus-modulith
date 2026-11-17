package io.quarkiverse.modulith.moments.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.moments.HourHasPassed;
import io.quarkiverse.modulith.moments.deployment.fixtures.Jobs;
import io.quarkiverse.modulith.moments.deployment.fixtures.RecordedMoments;
import io.quarkiverse.modulith.moments.deployment.fixtures.TestClock;
import io.quarkiverse.modulith.moments.support.TimeMachine;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusExtensionTest;

class JobsDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(RecordedMoments.class, TestClock.class, Jobs.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideConfigKey("quarkus.modulith.moments.enable-time-machine", "true")
            .overrideConfigKey("quarkus.modulith.moments.jobs.enabled", "false")
            .setLogRecordPredicate(record -> record.getLoggerName().startsWith("io.quarkiverse.modulith.moments"))
            .assertLogRecords(records -> assertThat(records).extracting(LogRecord::getMessage)
                    .containsExactly("%s is false, so this instance publishes no passage of time events on its own"));

    @Inject
    Scheduler scheduler;

    @Inject
    TimeMachine timeMachine;

    @Inject
    RecordedMoments recorded;

    @Test
    void registersNoJobButShiftsTime() {
        // act
        timeMachine.shiftBy(Duration.ofHours(1));

        // assert
        assertThat(scheduler.isStarted()).isTrue();
        assertThat(Jobs.identities(scheduler)).isEmpty();
        assertThat(recorded.events()).containsExactly(HourHasPassed.of(LocalDateTime.of(2026, 3, 31, 21, 0)));
    }
}
