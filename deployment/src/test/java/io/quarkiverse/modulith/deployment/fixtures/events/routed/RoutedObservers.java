package io.quarkiverse.modulith.deployment.fixtures.events.routed;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Singleton;

/**
 * Valid CDI observers the event publication registry cannot call.
 */
@Singleton
public class RoutedObservers {

    static void onStatic(@Observes(during = TransactionPhase.AFTER_SUCCESS) Shipment event) {
    }

    private void onPrivate(@Observes(during = TransactionPhase.AFTER_SUCCESS) Shipment event) {
    }

    static void onTracked(@Observes(during = TransactionPhase.AFTER_SUCCESS) @Tracked Shipment event) {
    }
}
