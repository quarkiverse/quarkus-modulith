package io.quarkiverse.modulith.events.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.deployment.memory.Charged;
import io.quarkiverse.modulith.events.deployment.memory.FlakyListener;
import io.quarkiverse.modulith.events.deployment.memory.MemoryRepository;
import io.quarkus.test.QuarkusExtensionTest;

public class ScheduledResubmissionTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackage(Charged.class.getPackage()))
            .overrideConfigKey("quarkus.modulith.base-package", Charged.class.getPackageName())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideConfigKey("quarkus.modulith.events.republish-interval", "1s")
            .overrideRuntimeConfigKey("quarkus.modulith.events.republish-older-than", "0s")
            .overrideRuntimeConfigKey("quarkus.modulith.events.staleness.processing", "1h");

    @Inject
    Event<Charged> events;

    @Inject
    MemoryRepository repository;

    @Test
    void resubmitsTheFailedPublicationOnTheSchedule() {
        // arrange
        FlakyListener.HANDLED.clear();
        FlakyListener.FAILURES.set(1);

        // act
        events.fire(new Charged("c-1"));

        // assert
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(FlakyListener.HANDLED).containsExactly("c-1");
            assertThat(repository.findByStatus(Status.COMPLETED)).hasSize(1);
        });
    }
}
