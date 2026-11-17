package io.quarkiverse.modulith.moments.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.moments.deployment.fixtures.Jobs;
import io.quarkus.scheduler.Scheduler;
import io.quarkus.test.QuarkusExtensionTest;

class DaysGranularityTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Jobs.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideConfigKey("quarkus.modulith.moments.granularity", "days");

    @Inject
    Scheduler scheduler;

    @Test
    void schedulesTheDailyJobOnly() {
        // act
        var identities = Jobs.identities(scheduler);

        // assert
        assertThat(identities).containsExactly("modulith-moments-days");
    }
}
