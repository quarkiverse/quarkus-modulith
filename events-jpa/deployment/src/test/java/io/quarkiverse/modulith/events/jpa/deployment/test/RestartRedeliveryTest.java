package io.quarkiverse.modulith.events.jpa.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.CompletedEventPublications;
import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.jpa.deployment.crashed.CrashedInstance;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.BillingApplication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.invoice.internal.InvoiceListener;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderPlaced;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Only {@code republish-outstanding-events-on-restart} is set: no staleness threshold and no periodic job, the
 * default configuration otherwise.
 */
public class RestartRedeliveryTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, BillingApplication.class.getPackage())
                    .addClass(CrashedInstance.class))
            .withConfigurationResource("application-h2.properties")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideRuntimeConfigKey("quarkus.modulith.events.republish-outstanding-events-on-restart", "true");

    @Inject
    CompletedEventPublications completed;

    @Test
    void deliversAgainWhatAnInstanceLeftProcessingWhenItDied() {
        // arrange
        OrderPlaced left = new OrderPlaced("crashed-1", 7);

        // act
        await().atMost(Duration.ofSeconds(30)).until(() -> !completed.findAll().isEmpty());

        // assert
        assertThat(InvoiceListener.INVOICED).containsExactly(left);
        assertThat(completed.findAll()).singleElement().satisfies(publication -> {
            assertThat(publication.getIdentifier()).isEqualTo(CrashedInstance.left);
            assertThat(publication.getStatus()).isEqualTo(Status.COMPLETED);
            assertThat(publication.getCompletionAttempts()).isEqualTo(2);
        });
    }
}
