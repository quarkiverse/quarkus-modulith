package io.quarkiverse.modulith.deployment.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Map;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.modulith.deployment.fixtures.events.propagation.PropagationListeners;
import io.quarkiverse.modulith.deployment.fixtures.events.propagation.Recorded;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.QuarkusTransactionException;
import io.quarkus.test.QuarkusExtensionTest;

public class ListenerPropagationTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackage(Recorded.class.getPackage()))
            .overrideConfigKey("quarkus.modulith.base-package", Recorded.class.getPackageName())
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.http.test-port", "0");

    @Inject
    Event<Recorded> events;

    @Test
    void runsEachListenerWithTheTransactionItDeclares() {
        // arrange
        PropagationListeners.IN_TRANSACTION.clear();

        // act
        QuarkusTransaction.requiringNew().run(() -> events.fire(new Recorded("r-1")));

        // assert
        assertThat(PropagationListeners.IN_TRANSACTION).containsExactlyInAnyOrderEntriesOf(Map.of(
                "REQUIRED", true, "REQUIRES_NEW", true, "SUPPORTS", false, "NOT_SUPPORTED", false, "NEVER", false));
    }

    @Test
    void dropsDeliveriesOfATransactionMarkedForRollback() {
        // arrange
        PropagationListeners.IN_TRANSACTION.clear();

        // act
        Throwable failure = catchThrowable(() -> QuarkusTransaction.requiringNew().run(() -> {
            QuarkusTransaction.setRollbackOnly();
            events.fire(new Recorded("r-2"));
        }));

        // assert
        assertThat(PropagationListeners.IN_TRANSACTION).isEmpty();
        assertThat(failure).isInstanceOf(QuarkusTransactionException.class)
                .hasMessageContaining("ARJUNA016053: Could not commit transaction.");
    }
}
