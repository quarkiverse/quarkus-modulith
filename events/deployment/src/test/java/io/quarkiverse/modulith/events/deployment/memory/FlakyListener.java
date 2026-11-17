package io.quarkiverse.modulith.events.deployment.memory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import io.quarkiverse.modulith.ApplicationModuleListener;

public class FlakyListener {

    public static final AtomicInteger FAILURES = new AtomicInteger();
    public static final List<String> HANDLED = new CopyOnWriteArrayList<>();

    @ApplicationModuleListener(id = "flaky")
    void on(Charged event) {
        if (FAILURES.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
            throw new IllegalStateException("flaky " + event.id());
        }
        HANDLED.add(event.id());
    }
}
