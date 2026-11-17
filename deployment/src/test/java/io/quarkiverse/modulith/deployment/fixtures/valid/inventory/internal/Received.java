package io.quarkiverse.modulith.deployment.fixtures.valid.inventory.internal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.inject.Singleton;

@Singleton
public class Received {

    private final List<Object> events = new CopyOnWriteArrayList<>();
    private final List<String> threads = new CopyOnWriteArrayList<>();
    private final List<Boolean> inTransaction = new CopyOnWriteArrayList<>();

    public void add(Object event, boolean transactional) {
        events.add(event);
        threads.add(Thread.currentThread().getName());
        inTransaction.add(transactional);
    }

    public List<Object> events() {
        return events;
    }

    public List<String> threads() {
        return threads;
    }

    public List<Boolean> inTransaction() {
        return inTransaction;
    }

    public void clear() {
        events.clear();
        threads.clear();
        inTransaction.clear();
    }
}
