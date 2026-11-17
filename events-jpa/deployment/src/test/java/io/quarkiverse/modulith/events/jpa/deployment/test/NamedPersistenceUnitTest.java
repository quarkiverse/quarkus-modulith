package io.quarkiverse.modulith.events.jpa.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.CompletedEventPublications;
import io.quarkiverse.modulith.events.EventPublication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.BillingApplication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.invoice.internal.InvoiceListener;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderService;
import io.quarkiverse.modulith.runtime.events.ModuleEventDispatcher;
import io.quarkus.test.QuarkusExtensionTest;

public class NamedPersistenceUnitTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackage(BillingApplication.class.getPackage())
                    .addPackage(OrderService.class.getPackage()).addPackage(InvoiceListener.class.getPackage()))
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:named;DB_CLOSE_DELAY=-1")
            .overrideConfigKey("quarkus.hibernate-orm.\"outbox\".datasource", "<default>")
            .overrideConfigKey("quarkus.hibernate-orm.\"outbox\".schema-management.strategy", "drop-and-create")
            .overrideConfigKey("quarkus.hibernate-orm.\"outbox\".packages", "io.quarkiverse.modulith.events.jpa")
            .overrideConfigKey("quarkus.modulith.events.jpa.persistence-unit", "outbox")
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0")
            .overrideRuntimeConfigKey("quarkus.modulith.events.republish-interval", "1h");

    @Inject
    OrderService orders;

    @Inject
    CompletedEventPublications completed;

    @Inject
    ModuleEventDispatcher dispatcher;

    @Test
    void storesAndCompletesPublicationsInTheNamedUnit() {
        // arrange
        InvoiceListener.INVOICED.clear();
        InvoiceListener.FAILURES_TO_SIMULATE.set(0);

        // act
        orders.place("n-1", 1);
        boolean idle = dispatcher.awaitIdle(Duration.ofSeconds(10));

        // assert
        assertThat(idle).isTrue();
        assertThat(completed.findAll()).extracting(EventPublication::getListenerId)
                .containsExactly("invoice.on-order-placed");
    }
}
