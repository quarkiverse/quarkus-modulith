package io.quarkiverse.modulith.events.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.deployment.memory.Charged;
import io.quarkiverse.modulith.events.registry.SchedulerPeriodicJobs;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;

public class RuntimeOnlyIntervalTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackage(Charged.class.getPackage()))
            .overrideConfigKey("quarkus.modulith.base-package", Charged.class.getPackageName())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideRuntimeConfigKey("quarkus.modulith.events.republish-interval", "1s")
            .setLogRecordPredicate(record -> record.getLevel().intValue() == Level.WARNING.intValue()
                    && record.getLoggerName().startsWith("io.quarkiverse.modulith"))
            .assertLogRecords(records -> assertThat(records).extracting(record -> record.getMessage())
                    .contains("Job %s is configured but no scheduler runs it: add quarkus-scheduler, and set the"
                            + " interval in application.properties, where the build sees it"));

    @Test
    void registersNoSchedulerJobs() {
        // arrange
        var container = Arc.container();

        // act
        boolean jobs = container.instance(SchedulerPeriodicJobs.class).isAvailable();

        // assert
        assertThat(jobs).isFalse();
    }
}
