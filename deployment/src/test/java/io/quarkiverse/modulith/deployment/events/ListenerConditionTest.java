package io.quarkiverse.modulith.deployment.events;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.events.conditions.ConditionListeners;
import io.quarkiverse.modulith.deployment.fixtures.events.conditions.Customer;
import io.quarkiverse.modulith.deployment.fixtures.events.conditions.Parcel;
import io.quarkiverse.modulith.deployment.fixtures.events.conditions.Payment;
import io.quarkiverse.modulith.deployment.fixtures.events.conditions.PaymentState;
import io.quarkiverse.modulith.deployment.fixtures.events.conditions.Signal;
import io.quarkus.test.QuarkusExtensionTest;

public class ListenerConditionTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackage(Payment.class.getPackage()))
            .overrideConfigKey("quarkus.modulith.base-package", Payment.class.getPackageName())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    Event<Payment> payments;

    @Inject
    Event<Object> others;

    @Test
    void deliversOnlyToListenersWhoseConditionHolds() {
        // arrange
        ConditionListeners.HITS.clear();
        Payment r1 = new Payment("r-1", 150, "EUR", new Customer("Ann", true), false, PaymentState.OPEN);
        Payment r2 = new Payment("r-2", 7, null, new Customer("Bob", false), true, PaymentState.CLOSED);
        Payment r3 = new Payment("r-3", 500, "USD", new Customer("Cy", true), true, PaymentState.CLOSED);

        // act
        payments.fire(r1);
        payments.fire(r2);
        payments.fire(r3);

        // assert
        assertThat(ConditionListeners.HITS).containsExactlyInAnyOrder(
                "large:r-1", "euro:r-1", "vip:r-1", "open:r-1", "ann:r-1", "range:r-1", "literals:r-1", "audit:r-1",
                "even:r-1", "dependent:r-1", "dependent:r-3",
                "small:r-2", "urgent:r-2", "literals:r-2", "closed:r-2", "sub:r-2",
                "large:r-3", "urgent:r-3", "literals:r-3", "even:r-3", "even-urgent:r-3", "sub:r-3");
    }

    @Test
    void resolvesAccessorsThroughInterfacesAndSkipsMethodsThatAreNoAccessors() {
        // arrange
        ConditionListeners.HITS.clear();

        // act
        others.fire(new Parcel("fragile"));
        others.fire(new Parcel("plain"));
        others.fire(new Signal(true, "x", new String[0]));
        others.fire(new Signal(true, "x", null));
        others.fire(new Signal(false, "x", new String[0]));

        // assert
        assertThat(ConditionListeners.HITS).containsExactly("labeled:fragile", "signal:x");
    }
}
