package io.quarkiverse.modulith.events.jpa.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.CompletedEventPublications;
import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.IncompleteEventPublications;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.BillingApplication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.invoice.internal.InvoiceListener;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderPlaced;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderService;
import io.quarkus.test.QuarkusExtensionTest;

public class PeriodicRepublicationTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, BillingApplication.class.getPackage()))
            .withConfigurationResource("application-h2.properties")
            .overrideConfigKey("quarkus.modulith.events.republish-interval", "1s")
            .overrideRuntimeConfigKey("quarkus.modulith.events.republish-older-than", "0s")
            .overrideRuntimeConfigKey("quarkus.modulith.events.staleness.processing", "1h");

    @Inject
    OrderService orders;

    @Inject
    IncompleteEventPublications incomplete;

    @Inject
    CompletedEventPublications completed;

    @Test
    void resubmitsFailedPublicationsUntilTheyComplete() {
        // arrange
        InvoiceListener.INVOICED.clear();
        InvoiceListener.FAILURES_TO_SIMULATE.set(2);

        // act
        orders.place("r-1", 10);

        // assert
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(InvoiceListener.INVOICED).containsExactly(new OrderPlaced("r-1", 10)));
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(incomplete.findIncompletePublications()).isEmpty());
        assertThat(completed.findAll()).singleElement().satisfies(publication -> {
            assertThat(publication.getStatus()).isEqualTo(Status.COMPLETED);
            assertThat(publication.getCompletionAttempts()).isEqualTo(3);
            assertThat(publication.getCompletionDate()).isPresent();
        });
    }
}
