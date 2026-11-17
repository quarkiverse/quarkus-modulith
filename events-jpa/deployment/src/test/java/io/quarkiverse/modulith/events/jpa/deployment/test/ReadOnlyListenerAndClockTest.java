package io.quarkiverse.modulith.events.jpa.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.CompletedEventPublications;
import io.quarkiverse.modulith.events.EventPublication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.BillingApplication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderService;
import io.quarkiverse.modulith.events.jpa.deployment.readonly.FixedClock;
import io.quarkiverse.modulith.events.jpa.deployment.readonly.ReadOnlyListener;
import io.quarkiverse.modulith.runtime.events.ModuleEventDispatcher;
import io.quarkus.test.QuarkusExtensionTest;

public class ReadOnlyListenerAndClockTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, BillingApplication.class.getPackage())
                    .addPackage(ReadOnlyListener.class.getPackage()))
            .withConfigurationResource("application-h2.properties")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    OrderService orders;

    @Inject
    CompletedEventPublications completed;

    @Inject
    ModuleEventDispatcher dispatcher;

    @Test
    void runsReadOnlyListenersInAReadOnlySessionAndDatesWithTheApplicationClock() {
        // arrange
        ReadOnlyListener.SEEN.clear();

        // act
        orders.place("ro-1", 10);
        boolean idle = dispatcher.awaitIdle(Duration.ofSeconds(10));

        // assert
        assertThat(idle).isTrue();
        assertThat(ReadOnlyListener.SEEN).containsExactly("read-only=true,manual=true");
        assertThat(completed.findAll()).extracting(EventPublication::getListenerId, EventPublication::getPublicationDate)
                .containsExactlyInAnyOrder(tuple("read-only", FixedClock.NOW),
                        tuple("invoice.on-order-placed", FixedClock.NOW));
    }
}
