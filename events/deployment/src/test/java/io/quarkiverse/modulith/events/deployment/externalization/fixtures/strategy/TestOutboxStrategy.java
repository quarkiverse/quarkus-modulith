package io.quarkiverse.modulith.events.deployment.externalization.fixtures.strategy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.externalization.ExternalizationStrategy;
import io.quarkus.arc.Unremovable;

@Singleton
@Unremovable
public class TestOutboxStrategy implements ExternalizationStrategy {

    private final List<Object> events = new CopyOnWriteArrayList<>();

    @Override
    public void onEvent(Object event) {
        events.add(event);
    }

    public List<Object> events() {
        return List.copyOf(events);
    }
}
