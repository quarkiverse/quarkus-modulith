package io.quarkiverse.modulith.events.messaging.deployment.fixtures.support;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Singleton;

@Singleton
public class AckTracker {

    private final AtomicInteger acknowledged = new AtomicInteger();

    public void acknowledge() {
        acknowledged.incrementAndGet();
    }

    public int acknowledged() {
        return acknowledged.get();
    }
}
