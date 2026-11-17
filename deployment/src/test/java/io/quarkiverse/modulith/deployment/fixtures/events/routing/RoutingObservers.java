package io.quarkiverse.modulith.deployment.fixtures.events.routing;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;

import io.quarkiverse.modulith.deployment.fixtures.events.observers.Important;
import io.quarkiverse.modulith.deployment.fixtures.events.observers.Marker;

/**
 * Observers of every shape the event publication registry may or may not route.
 */
public class RoutingObservers {

    void routed(@Observes(during = TransactionPhase.AFTER_SUCCESS) String event) {
    }

    void ifExists(@Observes(during = TransactionPhase.AFTER_SUCCESS, notifyObserver = Reception.IF_EXISTS) String event) {
    }

    void always(@Observes(during = TransactionPhase.AFTER_SUCCESS, notifyObserver = Reception.ALWAYS) Long event) {
    }

    void inProgress(@Observes(during = TransactionPhase.IN_PROGRESS) String event) {
    }

    void plain(@Observes String event) {
    }

    void qualified(@Observes(during = TransactionPhase.AFTER_SUCCESS) @Important String event) {
    }

    void marked(@Observes(during = TransactionPhase.AFTER_SUCCESS) @Marker Short event) {
    }

    void twoParameters(@Observes(during = TransactionPhase.AFTER_SUCCESS) String event, Object other) {
    }

    @Trigger
    void triggered(@Observes(during = TransactionPhase.AFTER_SUCCESS) Integer event) {
    }
}
