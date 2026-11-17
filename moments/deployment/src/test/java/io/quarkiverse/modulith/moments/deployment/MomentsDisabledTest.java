package io.quarkiverse.modulith.moments.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.moments.support.Moments;
import io.quarkiverse.modulith.moments.support.Now;
import io.quarkiverse.modulith.moments.support.TimeMachine;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusExtensionTest;

class MomentsDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideConfigKey("quarkus.modulith.moments.enabled", "false")
            .overrideConfigKey("quarkus.modulith.moments.enable-time-machine", "true");

    @Inject
    Instance<Moments> moments;

    @Inject
    Instance<Now> now;

    @Inject
    Instance<TimeMachine> timeMachines;

    @Inject
    Scheduler scheduler;

    @Test
    void registersNothing() {
        // act
        boolean momentsMissing = moments.isUnsatisfied();
        boolean nowMissing = now.isUnsatisfied();
        boolean timeMachineMissing = timeMachines.isUnsatisfied();

        // assert
        assertThat(momentsMissing).isTrue();
        assertThat(nowMissing).isTrue();
        assertThat(timeMachineMissing).isTrue();
        assertThat(scheduler.isStarted()).isFalse();
    }
}
