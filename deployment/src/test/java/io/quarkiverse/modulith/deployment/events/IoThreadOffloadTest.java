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
import io.vertx.core.Vertx;

public class IoThreadOffloadTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackage(Ping.class.getPackage()))
            .overrideConfigKey("quarkus.modulith.base-package", Ping.class.getPackageName())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    Event<Ping> events;

    @Inject
    Vertx vertx;

    @Test
    void movesListenersFiredOnTheEventLoopToAWorkerThread() {
        // arrange
        ThreadListeners.CALLS.clear();

        // act
        vertx.runOnContext(ignored -> events.fire(new Ping("p-1")));

        // assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(ThreadListeners.CALLS)
                .containsExactlyInAnyOrder("sync:p-1:virtual=false:blocking=true",
                        "async:p-1:virtual=false:blocking=true"));
    }
}
