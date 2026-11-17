package io.quarkiverse.modulith.events.externalization;

import java.util.UUID;

/**
 * Decides what happens to an application event that may be externalized: the module listener mode sends it after the
 * publishing transaction commits, the outbox modes record it in the publishing transaction and send it later. Exactly
 * one bean of this type exists when externalization is enabled. Extensions that bring their own outbox, such as
 * {@code quarkus-modulith-events-jobrunr}, replace the default outbox strategy at build time.
 * <p>
 * This is a service provider interface for Quarkus Modulith extensions, not for applications.
 */
public interface ExternalizationStrategy {

    /**
     * Called for every application event that may be externalized, on the publishing thread, inside the publishing
     * transaction when there is one.
     */
    void onEvent(Object event);

    /**
     * @return whether event publications stored for the given listener identifier belong to this strategy, so that
     *         the event publication registry resubmits them through {@link #resubmit(UUID, Object)}
     */
    default boolean handles(String listenerId) {
        return false;
    }

    /**
     * Sends a stored publication again. Only called for listener identifiers this strategy {@link #handles(String)
     * handles}.
     */
    default void resubmit(UUID publicationId, Object event) {
        throw new UnsupportedOperationException(getClass().getName() + " stores no event publications");
    }
}
