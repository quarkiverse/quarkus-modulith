package io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.EventExternalized;

@Singleton
public class ExternalizedEvents {

    private final List<Object> brokerResults = new CopyOnWriteArrayList<>();

    void on(@Observes EventExternalized<?, ?> event) {
        brokerResults.add(event.getBrokerResult());
    }

    public List<Object> brokerResults() {
        return List.copyOf(brokerResults);
    }

    public void reset() {
        brokerResults.clear();
    }
}
