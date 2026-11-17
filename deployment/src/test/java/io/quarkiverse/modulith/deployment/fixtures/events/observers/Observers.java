package io.quarkiverse.modulith.deployment.fixtures.events.observers;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Named;

public class Observers {

    void plain(@Observes String event) {
    }

    void prioritized(@Observes @Priority(1) String event) {
    }

    void marked(@Observes @Marker String event) {
    }

    void important(@Observes @Important String event) {
    }

    void named(@Observes @Named("x") String event) {
    }
}
