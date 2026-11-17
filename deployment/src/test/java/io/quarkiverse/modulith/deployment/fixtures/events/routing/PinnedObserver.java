package io.quarkiverse.modulith.deployment.fixtures.events.routing;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

@Pinned
public class PinnedObserver {

    void on(@Observes(during = TransactionPhase.AFTER_SUCCESS) String event) {
    }
}
