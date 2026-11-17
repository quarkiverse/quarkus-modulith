package io.quarkiverse.modulith.events.jpa.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.jpa.deployment.drain.ShortListener;
import io.quarkiverse.modulith.events.jpa.deployment.drain.ShutdownProbe;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.BillingApplication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderPlaced;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderService;
import io.quarkus.test.QuarkusExtensionTest;

public class ShutdownDrainTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(BillingApplication.class, OrderService.class, OrderPlaced.class)
                    .addPackage(ShortListener.class.getPackage()))
            .withConfigurationResource("application-h2.properties")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideRuntimeConfigKey("quarkus.modulith.listeners.shutdown-timeout", "10s")
            .setAfterAllCustomizer(
                    () -> assertThat(System.clearProperty(ShutdownProbe.PROPERTY)).isEqualTo("finished=1,incomplete=0"));

    @Inject
    OrderService orders;

    @Test
    void waitsForTheRunningListener() {
        // arrange
        ShortListener.STARTED.clear();

        // act
        orders.place("d-1", 1);

        // assert
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(ShortListener.STARTED).containsExactly("d-1"));
    }
}
