package io.quarkiverse.modulith.events.jpa.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.events.jpa.deployment.fixtures.BillingApplication;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.invoice.internal.InvoiceListener;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderService;
import io.quarkiverse.modulith.runtime.events.ModuleEventDispatcher;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusExtensionTest;

public class DeleteCompletionModeTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, BillingApplication.class.getPackage()))
            .withConfigurationResource("application-h2.properties")
            .overrideConfigKey("quarkus.modulith.events.completion-mode", "delete");

    @Inject
    OrderService orders;

    @Inject
    ModuleEventDispatcher dispatcher;

    @Inject
    EntityManager entityManager;

    @Test
    void removesThePublicationOnceCompleted() {
        // arrange
        InvoiceListener.FAILURES_TO_SIMULATE.set(0);

        // act
        orders.place("d-1", 1);
        boolean idle = dispatcher.awaitIdle(Duration.ofSeconds(10));

        // assert
        assertThat(idle).isTrue();
        long rows = QuarkusTransaction.requiringNew().call(
                () -> entityManager.createQuery("select count(p) from JpaEventPublication p", Long.class).getSingleResult());
        assertThat(rows).isZero();
    }
}
