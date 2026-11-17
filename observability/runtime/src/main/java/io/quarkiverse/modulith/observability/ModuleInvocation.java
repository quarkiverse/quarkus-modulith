package io.quarkiverse.modulith.observability;

/**
 * A call into an application module from outside it, the counterpart of Spring Modulith's {@code ModulithContext}.
 *
 * @param moduleIdentifier the identifier of the invoked module
 * @param moduleName the display name of the invoked module
 * @param method the name of the invoked method
 * @param invocationType {@code event-listener} for event listener methods, {@code bean} otherwise
 * @param contextualName the span name: {@code <module name> > <abbreviated type>.<method>(<parameters>)}
 */
public record ModuleInvocation(String moduleIdentifier, String moduleName, String method, String invocationType,
        String contextualName) {

    /**
     * @return whether the invoked method is an event listener method
     */
    public boolean isEventListener() {
        return "event-listener".equals(invocationType);
    }
}
