package io.quarkiverse.modulith.runtime.events;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.spi.ListenerExecutor;
import io.quarkus.virtual.threads.VirtualThreads;

/**
 * Runs each delivery on a new virtual thread, through the executor of {@code quarkus-virtual-threads}.
 * <p>
 * The {@code @VirtualThreads} executor runs a task in the Vert.x context of the thread that submitted it, and ArC
 * keeps the request context of an HTTP request in that context. Submitted from the publisher's thread, a listener
 * would join the publisher's request context instead of getting a fresh one, while the request goes on (or ends) on
 * the other thread. So the task is submitted from the worker pool: Quarkus gives a worker task a copy of the
 * publisher's Vert.x context without the CDI contexts, which keeps the rest (the OpenTelemetry context, for one).
 */
@Singleton
public class VirtualThreadListenerExecutor implements ListenerExecutor {

    @Inject
    ExecutorService worker;

    @Inject
    @VirtualThreads
    ExecutorService virtualThreads;

    @Override
    public void execute(Runnable task) {
        worker.execute(() -> start(task));
    }

    /**
     * The virtual thread executor only rejects once it is shut down. The task then runs on the worker thread, so the
     * delivery is not lost and its in-flight count is released.
     */
    private void start(Runnable task) {
        try {
            virtualThreads.execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        }
    }
}
