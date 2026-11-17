package io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.jobrunr.JobRunrExternalizationTransport;

@Singleton
public class RecordingJobRunrTransport implements JobRunrExternalizationTransport {

    private final List<Object> delivered = new CopyOnWriteArrayList<>();
    private final AtomicInteger attempts = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();

    @Override
    public void externalize(Object event) {
        attempts.incrementAndGet();
        if (failures.getAndUpdate(remaining -> Math.max(0, remaining - 1)) > 0) {
            throw new IllegalStateException("broker down");
        }
        delivered.add(event);
    }

    public List<Object> delivered() {
        return List.copyOf(delivered);
    }

    public int attempts() {
        return attempts.get();
    }

    public void failNext(int times) {
        failures.set(times);
    }

    public void reset() {
        delivered.clear();
        attempts.set(0);
        failures.set(0);
    }
}
