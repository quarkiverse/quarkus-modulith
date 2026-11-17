package io.quarkiverse.modulith.moments.deployment.fixtures;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.jmolecules.event.types.DomainEvent;

/**
 * Records every moment event, in the order the events are fired.
 */
@Singleton
public class RecordedMoments {

    private final List<Object> events = new CopyOnWriteArrayList<>();

    void on(@Observes DomainEvent event) {
        events.add(event);
    }

    public List<Object> events() {
        return List.copyOf(events);
    }

    public void clear() {
        events.clear();
    }
}
