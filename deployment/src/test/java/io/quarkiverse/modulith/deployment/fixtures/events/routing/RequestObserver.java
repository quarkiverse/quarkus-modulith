package io.quarkiverse.modulith.deployment.fixtures.events.routing;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

@RequestScoped
public class RequestObserver {

    void on(@Observes(during = TransactionPhase.AFTER_SUCCESS) String event) {
    }
}
