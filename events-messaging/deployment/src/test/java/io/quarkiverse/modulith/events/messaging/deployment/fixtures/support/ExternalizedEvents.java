package io.quarkiverse.modulith.events.messaging.deployment.fixtures.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.EventExternalized;

@Singleton
public class ExternalizedEvents {

    private final List<EventExternalized<?, ?>> received = new CopyOnWriteArrayList<>();

    void on(@Observes EventExternalized<?, ?> event) {
        received.add(event);
    }

    public List<EventExternalized<?, ?>> received() {
        return List.copyOf(received);
    }

    public void reset() {
        received.clear();
    }
}
