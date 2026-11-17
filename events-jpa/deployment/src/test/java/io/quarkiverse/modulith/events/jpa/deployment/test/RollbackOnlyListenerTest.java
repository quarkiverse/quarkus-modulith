package io.quarkiverse.modulith.events.jpa.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.CompletedEventPublications;
import io.quarkiverse.modulith.events.EventPublication;
import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.IncompleteEventPublications;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.BillingApplication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderService;
import io.quarkiverse.modulith.events.jpa.deployment.rollback.RollbackOnlyListener;
import io.quarkiverse.modulith.runtime.events.ModuleEventDispatcher;
import io.quarkus.test.QuarkusExtensionTest;

public class RollbackOnlyListenerTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, BillingApplication.class.getPackage())
                    .addPackage(RollbackOnlyListener.class.getPackage()))
            .withConfigurationResource("application-h2.properties")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    OrderService orders;

    @Inject
    IncompleteEventPublications incomplete;

    @Inject
    CompletedEventPublications completed;

    @Inject
    ModuleEventDispatcher dispatcher;

    @Test
    void marksThePublicationFailedWhenTheListenerMarksItsTransactionRollbackOnly() {
        // arrange
        String order = "rb-1";

        // act
        orders.place(order, 1);
        boolean idle = dispatcher.awaitIdle(Duration.ofSeconds(10));

        // assert
        assertThat(idle).isTrue();
        assertThat(incomplete.findIncompletePublications()).singleElement().satisfies(p -> {
            assertThat(p.getListenerId()).isEqualTo("rollback-only");
            assertThat(p.getStatus()).isEqualTo(Status.FAILED);
            assertThat(p.getCompletionAttempts()).isEqualTo(1);
            assertThat(p.getCompletionDate()).isEmpty();
        });
        assertThat(completed.findAll()).extracting(EventPublication::getListenerId)
                .containsExactly("invoice.on-order-placed");
    }
}
