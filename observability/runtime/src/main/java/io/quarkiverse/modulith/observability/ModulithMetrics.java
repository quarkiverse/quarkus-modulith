package io.quarkiverse.modulith.observability;

/**
 * The meter names and tags Spring Modulith uses for module events. The tags are the ones Spring Modulith's
 * {@code ModuleEventListener} writes, the lower-cased constant names of its {@code ModulithMetrics.LowKeys}
 * ({@code event_type}, {@code module_identifier}, {@code module_name}), not their {@code asString()} key names.
 */
public final class ModulithMetrics {

    /**
     * The counter of all module events, tagged with {@link #EVENT_TYPE}, {@link #MODULE_IDENTIFIER} and
     * {@link #MODULE_NAME}. Each event type also has its own counter named
     * {@code module.events.published.<module>.<SimpleTypeName>}.
     */
    public static final String ALL_EVENTS = "module.events.published";

    /**
     * Tag of {@link #ALL_EVENTS}: {@code <module>.<SimpleTypeName>} of the event.
     */
    public static final String EVENT_TYPE = "event_type";

    /**
     * Tag: the identifier of the module the event type belongs to.
     */
    public static final String MODULE_IDENTIFIER = "module_identifier";

    /**
     * Tag: the display name of the module the event type belongs to.
     */
    public static final String MODULE_NAME = "module_name";

    /**
     * Quarkus extra: gauge of the event publications per status, tagged with {@link #STATUS}.
     */
    public static final String PUBLICATIONS = "modulith.event.publications";

    /**
     * Tag of {@link #PUBLICATIONS}: the publication status in lower case.
     */
    public static final String STATUS = "status";

    private ModulithMetrics() {
    }
}
