package io.quarkiverse.modulith.it.externalizationjms.broker;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.EventExternalized;

@Singleton
public class Externalizations {

    private final List<Externalization> received = new CopyOnWriteArrayList<>();

    void on(@Observes EventExternalized<?, ?> event) {
        received.add(new Externalization(event.getEventType().getName(), event.getTarget().toString(),
                String.valueOf(event.getBrokerResult())));
    }

    public List<Externalization> received() {
        return List.copyOf(received);
    }
}
