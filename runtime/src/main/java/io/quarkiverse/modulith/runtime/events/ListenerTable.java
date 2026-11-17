package io.quarkiverse.modulith.runtime.events;

import java.util.List;

/**
 * The module listeners found at build time, exposed as a synthetic bean and injected into the
 * {@link ModuleEventDispatcher}.
 *
 * @param listeners the listeners
 */
public record ListenerTable(List<ListenerData> listeners) {

    public ListenerTable {
        listeners = List.copyOf(listeners);
    }
}
