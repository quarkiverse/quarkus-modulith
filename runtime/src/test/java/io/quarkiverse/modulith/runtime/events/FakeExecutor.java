package io.quarkiverse.modulith.runtime.events;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import io.quarkiverse.modulith.events.spi.ListenerExecutor;

/**
 * Queues tasks until {@link #runAll()}, or rejects them.
 */
final class FakeExecutor implements ListenerExecutor {

    final List<Runnable> queued = new ArrayList<>();
    boolean reject;

    @Override
    public void execute(Runnable task) {
        if (reject) {
            throw new RejectedExecutionException("full");
        }
        queued.add(task);
    }

    void runAll() {
        List<Runnable> tasks = List.copyOf(queued);
        queued.clear();
        tasks.forEach(Runnable::run);
    }
}
