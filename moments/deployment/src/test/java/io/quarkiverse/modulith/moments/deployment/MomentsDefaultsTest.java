package io.quarkiverse.modulith.moments.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.moments.deployment.fixtures.Jobs;
import io.quarkiverse.modulith.moments.deployment.fixtures.RecordedMoments;
import io.quarkiverse.modulith.moments.support.Moments;
import io.quarkiverse.modulith.moments.support.Now;
import io.quarkiverse.modulith.moments.support.TimeMachine;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusExtensionTest;

class MomentsDefaultsTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(RecordedMoments.class, Jobs.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    Moments moments;

    @Inject
    Now now;

    @Inject
    Instance<TimeMachine> timeMachines;

    @Inject
    Scheduler scheduler;

    @Test
    void registersMomentsAsNow() {
        // act
        Class<?> type = moments.getClass();

        // assert
        assertThat(type).isEqualTo(Moments.class);
        assertThat(now).isSameAs(moments);
        assertThat(timeMachines.isUnsatisfied()).isTrue();
    }

    @Test
    void schedulesTheHourlyAndDailyJobs() {
        // act
        var identities = Jobs.identities(scheduler);

        // assert
        assertThat(identities).containsExactly("modulith-moments-days", "modulith-moments-hours");
    }

    @Test
    void usesTheSystemClockInUtc() {
        // arrange
        Instant before = Instant.now();

        // act
        Instant instant = moments.instant();
        LocalDateTime local = moments.now();
        Instant after = Instant.now();

        // assert
        assertThat(instant).isBetween(before, after);
        assertThat(local).isBetween(LocalDateTime.ofInstant(before, ZoneOffset.UTC),
                LocalDateTime.ofInstant(after, ZoneOffset.UTC));
    }
}
