package io.quarkiverse.modulith.deployment;

import java.util.List;
import java.util.Map;

import org.jboss.jandex.Type;

import io.quarkiverse.modulith.runtime.events.ListenerData;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The {@code @ApplicationModuleListener} methods of the application.
 */
public final class ModuleListenersBuildItem extends SimpleBuildItem {

    private final List<ListenerData> listeners;
    private final Map<String, Type> observedTypes;

    public ModuleListenersBuildItem(List<ListenerData> listeners) {
        this(listeners, Map.of());
    }

    /**
     * @param observedTypes the event parameter types, keyed by {@link ListenerData#observedType()}
     */
    public ModuleListenersBuildItem(List<ListenerData> listeners, Map<String, Type> observedTypes) {
        this.listeners = List.copyOf(listeners);
        this.observedTypes = Map.copyOf(observedTypes);
    }

    public List<ListenerData> getListeners() {
        return listeners;
    }

    /**
     * @return the event types the dispatcher observes, keyed by {@link ListenerData#observedType()}
     */
    public Map<String, Type> getObservedTypes() {
        return observedTypes;
    }
}
