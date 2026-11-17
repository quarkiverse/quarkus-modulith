package io.quarkiverse.modulith.observability.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkiverse.modulith.observability.deployment.hierarchy.HierarchyApplication;
import io.quarkiverse.modulith.observability.deployment.hierarchy.ledger.Ledger;
import io.quarkiverse.modulith.observability.deployment.hierarchy.payment.PaymentReceived;
import io.quarkiverse.modulith.observability.deployment.hierarchy.payment.PaymentRefunded;
import io.quarkiverse.modulith.observability.deployment.hierarchy.payment.Payments;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The ledger listens to the {@code PaymentEvent} interface, the payments module publishes its two implementations, so
 * the module model lists all three types.
 */
public class EventHierarchyTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, HierarchyApplication.class.getPackage())
                    .addClasses(TestTelemetry.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    Payments payments;

    @Inject
    Ledger ledger;

    @Inject
    MeterRegistry meters;

    @Test
    void countsEachEventOnceWhenASupertypeIsObservedToo() {
        // act
        payments.receive("1");
        payments.refund("2");

        // assert
        assertThat(ledger.entries()).containsExactly(new PaymentReceived("1"), new PaymentRefunded("2"));
        assertThat(meters.get("module.events.published").tags("event_type", "payment.PaymentReceived").counter()
                .count()).isEqualTo(1);
        assertThat(meters.get("module.events.published").tags("event_type", "payment.PaymentRefunded").counter()
                .count()).isEqualTo(1);
        assertThat(meters.get("module.events.published.payment.PaymentReceived").counter().count()).isEqualTo(1);
    }
}
