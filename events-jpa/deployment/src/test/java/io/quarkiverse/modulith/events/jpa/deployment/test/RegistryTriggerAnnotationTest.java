package io.quarkiverse.modulith.events.jpa.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.CompletedEventPublications;
import io.quarkiverse.modulith.events.EventPublication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.BillingApplication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderService;
import io.quarkiverse.modulith.events.jpa.deployment.routed.Outboxed;
import io.quarkiverse.modulith.events.jpa.deployment.routed.RoutedObservers;
import io.quarkiverse.modulith.runtime.events.ModuleEventDispatcher;
import io.quarkus.test.QuarkusExtensionTest;

public class RegistryTriggerAnnotationTest {

    static final String ROUTED = RoutedObservers.class.getName();
    static final String EVENT = "(io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderPlaced)";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(BillingApplication.class).addPackage(OrderService.class.getPackage())
                    .addPackage(RoutedObservers.class.getPackage()))
            .withConfigurationResource("application-h2.properties")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideConfigKey("quarkus.modulith.events.registry-trigger-annotation", Outboxed.class.getName());

    @Inject
    OrderService orders;

    @Inject
    CompletedEventPublications completed;

    @Inject
    ModuleEventDispatcher dispatcher;

    @Test
    void routesOnlyObserversCarryingTheTriggerAnnotation() {
        // arrange
        RoutedObservers.CALLS.clear();

        // act
        orders.place("t-1", 1);
        boolean idle = dispatcher.awaitIdle(Duration.ofSeconds(10));

        // assert
        assertThat(idle).isTrue();
        assertThat(RoutedObservers.CALLS).containsExactlyInAnyOrder("in-progress:t-1", "after-completion:t-1",
                "after-success:t-1",
                "marked:t-1", "with-bean-manager:t-1", "prioritized:t-1");
        assertThat(completed.findAll()).extracting(EventPublication::getListenerId).containsExactlyInAnyOrder(
                ROUTED + ".marked" + EVENT);
    }
}
