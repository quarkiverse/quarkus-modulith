package io.quarkiverse.modulith.events;

/**
 * How externalized events reach the broker, like Spring Modulith's {@code ExternalizationMode}. Set with
 * {@value #PROPERTY}.
 */
public enum ExternalizationMode {

    /**
     * A module listener sends the event after the publishing transaction commits. The publication completes when the
     * broker acknowledges. This is the default.
     */
    MODULE_LISTENER,

    /**
     * The event is recorded in an outbox inside the publishing transaction: the event publication registry, or JobRunr
     * jobs when {@code quarkus-modulith-events-jobrunr} is present. A job drains it and sends the events in order.
     */
    OUTBOX;

    /**
     * The build-time property that selects the mode.
     */
    public static final String PROPERTY = "quarkus.modulith.events.externalization.mode";
}
