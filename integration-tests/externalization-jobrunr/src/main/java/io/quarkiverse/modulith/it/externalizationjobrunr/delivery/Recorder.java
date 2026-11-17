package io.quarkiverse.modulith.it.externalizationjobrunr.delivery;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Records what a transport delivered and fails on request, like a broker that is down.
 */
public class Recorder {

    private final List<Delivery> deliveries = new CopyOnWriteArrayList<>();
    private final AtomicInteger attempts = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();

    void record(String target, Object event) {
        attempts.incrementAndGet();
        if (failures.getAndUpdate(remaining -> Math.max(0, remaining - 1)) > 0) {
            throw new IllegalStateException("broker down");
        }
        deliveries.add(new Delivery(target, event));
    }

    List<Delivery> deliveries() {
        return List.copyOf(deliveries);
    }

    int attempts() {
        return attempts.get();
    }

    void failNext(int times) {
        failures.set(times);
    }

    void reset() {
        deliveries.clear();
        attempts.set(0);
        failures.set(0);
    }
}
