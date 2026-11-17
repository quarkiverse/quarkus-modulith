package io.quarkiverse.modulith.events.spi;

/**
 * Runs asynchronous listener deliveries. The deployment module registers the implementation selected by
 * {@code quarkus.modulith.listeners.executor}.
 */
public interface ListenerExecutor {

    /**
     * @throws java.util.concurrent.RejectedExecutionException if the task cannot be accepted
     */
    void execute(Runnable task);
}
