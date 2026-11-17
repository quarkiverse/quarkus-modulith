package io.quarkiverse.modulith.core;

/**
 * How the framework calls into a module.
 */
public enum EntryPointKind {

    /**
     * A Jakarta REST resource method or a Quarkus route.
     */
    HTTP,

    /**
     * A CDI observer method, a module listener or a Spring event listener.
     */
    EVENT_LISTENER,

    /**
     * A messaging consumer, such as {@code @Incoming}.
     */
    MESSAGING,

    /**
     * A scheduled method.
     */
    SCHEDULED
}
