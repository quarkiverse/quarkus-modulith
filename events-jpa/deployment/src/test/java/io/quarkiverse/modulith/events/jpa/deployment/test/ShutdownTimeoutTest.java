package io.quarkiverse.modulith.events.jpa.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.jpa.deployment.fixtures.BillingApplication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderPlaced;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderService;
import io.quarkiverse.modulith.events.jpa.deployment.stuck.ShutdownProbe;
import io.quarkiverse.modulith.events.jpa.deployment.stuck.StuckListener;
import io.quarkus.test.QuarkusExtensionTest;

public class ShutdownTimeoutTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(BillingApplication.class, OrderService.class, OrderPlaced.class)
                    .addPackage(StuckListener.class.getPackage()))
            .withConfigurationResource("application-h2.properties")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideConfigKey("quarkus.modulith.listeners.executor", "virtual-threads")
            .overrideRuntimeConfigKey("quarkus.modulith.listeners.shutdown-timeout", "200ms")
            .setAfterAllCustomizer(
                    () -> assertThat(System.clearProperty(ShutdownProbe.PROPERTY)).isEqualTo("finished=0,incomplete=1"));

    @Inject
    OrderService orders;

    @Test
    void leavesThePublicationIncompleteAfterTheTimeout() {
        // arrange
        StuckListener.STARTED.clear();

        // act
        orders.place("s-1", 1);

        // assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(StuckListener.STARTED).containsExactly("s-1"));
    }
}
