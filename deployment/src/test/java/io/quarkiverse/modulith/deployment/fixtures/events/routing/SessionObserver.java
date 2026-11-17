package io.quarkiverse.modulith.deployment.fixtures.events.routing;

import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

@SessionScoped
public class SessionObserver {

    void on(@Observes(during = TransactionPhase.AFTER_SUCCESS) String event) {
    }
}
