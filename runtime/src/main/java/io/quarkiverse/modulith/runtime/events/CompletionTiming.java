package io.quarkiverse.modulith.runtime.events;

/**
 * When a publication is marked completed, like Spring Modulith's {@code CompletionTiming}.
 */
public enum CompletionTiming {

    /**
     * In the listener's transaction, so the completion commits or rolls back with the listener's work.
     */
    IN_LISTENER,

    /**
     * In a separate transaction after the listener's transaction committed, as Spring Modulith 2.1 does.
     */
    IN_INTERCEPTOR
}
