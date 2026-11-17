package io.quarkiverse.modulith.deployment.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.events.threads.Ping;
import io.quarkiverse.modulith.deployment.fixtures.events.threads.ThreadListeners;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.VirtualThreadUnit;

@VirtualThreadUnit
@ShouldNotPin
public class VirtualThreadExecutorTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackage(Ping.class.getPackage()))
            .overrideConfigKey("quarkus.modulith.base-package", Ping.class.getPackageName())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideConfigKey("quarkus.modulith.listeners.executor", "virtual-threads");

    @Inject
    Event<Ping> events;

    @Test
    void runsAsynchronousListenersOnVirtualThreadsWithoutPinning() {
        // arrange
        ThreadListeners.CALLS.clear();

        // act
        events.fire(new Ping("p-2"));

        // assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(ThreadListeners.CALLS)
                .containsExactlyInAnyOrder("sync:p-2:virtual=false:blocking=true",
                        "async:p-2:virtual=true:blocking=true"));
    }
}
