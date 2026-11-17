package io.quarkiverse.modulith.observability;

/**
 * The names Spring Modulith uses for module invocations: the observation (and timer) name and the span attributes.
 */
public final class ModulithObservations {

    /**
     * The observation name, used for the timer of module invocations.
     */
    public static final String MODULE_INVOCATIONS = "module.invocations";

    /**
     * Span attribute and timer tag: the module identifier.
     */
    public static final String MODULE_IDENTIFIER = "module.identifier";

    /**
     * Span attribute and timer tag: the module display name.
     */
    public static final String MODULE_NAME = "module.name";

    /**
     * Span attribute and timer tag: {@code event-listener} or {@code bean}.
     */
    public static final String INVOCATION_TYPE = "module.invocation-type";

    /**
     * Span attribute: the invoked method name.
     */
    public static final String MODULE_METHOD = "module.method";

    private ModulithObservations() {
    }
}
