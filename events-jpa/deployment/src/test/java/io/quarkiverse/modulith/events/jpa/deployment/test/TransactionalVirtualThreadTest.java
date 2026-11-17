package io.quarkiverse.modulith.events.jpa.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.CompletedEventPublications;
import io.quarkiverse.modulith.events.EventPublication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.BillingApplication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.invoice.internal.InvoiceListener;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderService;
import io.quarkiverse.modulith.runtime.events.ModuleEventDispatcher;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.junit.virtual.ShouldNotPin;
import io.quarkus.test.junit.virtual.VirtualThreadUnit;

/**
 * Runs in a Surefire execution of its own ({@code virtual-threads} tag): the pinning check counts the events of the
 * whole JVM. A first delivery outside the measured window loads the classes and warms the JIT, so that a busy machine
 * does not turn a slow first delivery into pinning.
 */
@Tag("virtual-threads")
@VirtualThreadUnit
public class TransactionalVirtualThreadTest {

    private static final Duration IDLE_TIMEOUT = Duration.ofSeconds(60);

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, BillingApplication.class.getPackage()))
            .withConfigurationResource("application-h2.properties")
            .overrideConfigKey("quarkus.modulith.listeners.executor", "virtual-threads")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    OrderService orders;

    @Inject
    CompletedEventPublications completed;

    @Inject
    ModuleEventDispatcher dispatcher;

    @BeforeAll
    static void warmUp() {
        Arc.container().instance(OrderService.class).get().place("warm-up", 1);
        assertThat(Arc.container().instance(ModuleEventDispatcher.class).get().awaitIdle(IDLE_TIMEOUT)).isTrue();
    }

    @Test
    @ShouldNotPin
    void runsATransactionalListenerAndItsCompletionOnAVirtualThreadWithoutPinning() {
        // arrange
        InvoiceListener.INVOICED.clear();
        InvoiceListener.FAILURES_TO_SIMULATE.set(0);
        completed.deletePublications(publication -> true);

        // act
        orders.place("vt-1", 3);
        boolean idle = dispatcher.awaitIdle(IDLE_TIMEOUT);

        // assert
        assertThat(idle).isTrue();
        assertThat(InvoiceListener.INVOICED).extracting(event -> event.orderId()).containsExactly("vt-1");
        assertThat(completed.findAll()).extracting(EventPublication::getListenerId)
                .containsExactly("invoice.on-order-placed");
    }
}
