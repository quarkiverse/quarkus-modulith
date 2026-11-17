package io.quarkiverse.modulith.moments.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.moments.SecondHasPassed;
import io.quarkiverse.modulith.moments.deployment.fixtures.Jobs;
import io.quarkiverse.modulith.moments.deployment.fixtures.RecordedMoments;
import io.quarkiverse.modulith.moments.deployment.fixtures.TestClock;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusExtensionTest;

class SecondsGranularityTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(RecordedMoments.class, TestClock.class, Jobs.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideConfigKey("quarkus.modulith.moments.granularity", "seconds");

    @Inject
    Scheduler scheduler;

    @Inject
    RecordedMoments recorded;

    @Test
    void schedulesEveryJob() {
        // act
        var identities = Jobs.identities(scheduler);

        // assert
        assertThat(identities).containsExactly("modulith-moments-days", "modulith-moments-hours",
                "modulith-moments-minutes", "modulith-moments-seconds");
    }

    @Test
    void publishesTheSecondThatHasJustPassed() {
        // act
        await().atMost(Duration.ofSeconds(5)).until(() -> !recorded.events().isEmpty());

        // assert
        assertThat(recorded.events()).contains(SecondHasPassed.of(LocalDateTime.of(2026, 3, 31, 21, 58, 29)));
    }
}
