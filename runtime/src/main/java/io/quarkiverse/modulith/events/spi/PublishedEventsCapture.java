package io.quarkiverse.modulith.events.spi;

/**
 * Receives the application events published while the application runs in test mode. Test support implements it to
 * record published events, for {@code PublishedEvents} and {@code Scenario} of {@code quarkus-modulith-junit}.
 * <p>
 * Implementations are CDI beans that exist only in test mode: the extension registers the recorder of
 * {@code quarkus-modulith-junit} when the launch mode is {@code TEST} and that module is on the class path. The
 * recorder observes every event fired through CDI ({@code Event#fire} and {@code Event#fireAsync}) itself, with an
 * observer priority ahead of all others. The dispatcher neither records events nor notifies this interface. Other
 * components that publish events outside CDI may call {@link #onPublished(Object)} on every bean of this
 * type, on the publishing thread, before any listener runs.
 * <p>
 * An implementation must be fast, thread-safe, and must not throw.
 * <p>
 * This is a service provider interface for Quarkus Modulith extensions, not for applications.
 */
public interface PublishedEventsCapture {

    /**
     * @param event the published event, never {@code null}
     */
    void onPublished(Object event);
}
