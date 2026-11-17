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
import io.quarkiverse.modulith.events.jpa.deployment.readonlypublish.AuditLog;
import io.quarkiverse.modulith.runtime.events.ModuleEventDispatcher;
import io.quarkus.test.QuarkusExtensionTest;

public class ReadOnlyListenerPublishesTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, BillingApplication.class.getPackage())
                    .addPackage(AuditLog.class.getPackage()))
            .withConfigurationResource("application-h2.properties")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    OrderService orders;

    @Inject
    CompletedEventPublications completed;

    @Inject
    ModuleEventDispatcher dispatcher;

    @Test
    void storesTheEventsAReadOnlyListenerPublishes() {
        // arrange
        AuditLog.AUDITED.clear();

        // act
        orders.place("ro-publish-1", 3);
        boolean idle = dispatcher.awaitIdle(Duration.ofSeconds(60));

        // assert
        assertThat(idle).isTrue();
        assertThat(AuditLog.AUDITED).containsExactly("ro-publish-1");
        assertThat(completed.findAll()).extracting(EventPublication::getListenerId)
                .containsExactlyInAnyOrder("invoice.on-order-placed", "auditing", "audit-log");
    }
}
