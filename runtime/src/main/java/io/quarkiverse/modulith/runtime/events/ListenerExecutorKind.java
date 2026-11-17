package io.quarkiverse.modulith.runtime.events;

/**
 * Where asynchronous module listeners run.
 */
public enum ListenerExecutorKind {

    /**
     * The Quarkus worker pool.
     */
    WORKER,

    /**
     * One virtual thread per delivery, through the {@code @VirtualThreads} executor of {@code quarkus-virtual-threads}.
     */
    VIRTUAL_THREADS
}
