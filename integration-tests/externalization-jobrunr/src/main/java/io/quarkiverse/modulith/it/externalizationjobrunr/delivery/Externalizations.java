package io.quarkiverse.modulith.it.externalizationjobrunr.delivery;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.EventExternalized;

@Singleton
public class Externalizations {

    private final List<String> brokerResults = new CopyOnWriteArrayList<>();

    void on(@Observes EventExternalized<?, ?> event) {
        brokerResults.add(String.valueOf(event.getBrokerResult()));
    }

    List<String> brokerResults() {
        return List.copyOf(brokerResults);
    }

    void reset() {
        brokerResults.clear();
    }
}
