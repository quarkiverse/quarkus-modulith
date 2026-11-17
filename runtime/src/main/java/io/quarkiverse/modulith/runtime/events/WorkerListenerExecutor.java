package io.quarkiverse.modulith.runtime.events;

import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.spi.ListenerExecutor;

/**
 * Runs deliveries on the Quarkus worker pool.
 */
@Singleton
public class WorkerListenerExecutor implements ListenerExecutor {

    @Inject
    ExecutorService executor;

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }
}
