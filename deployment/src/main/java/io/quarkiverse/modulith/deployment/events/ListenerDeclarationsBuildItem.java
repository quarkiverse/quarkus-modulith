package io.quarkiverse.modulith.deployment.events;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The listener methods of the application, found once per build: the {@code @ApplicationModuleListener} methods and,
 * with an event publication registry, the routed {@code AFTER_SUCCESS} observers.
 */
public final class ListenerDeclarationsBuildItem extends SimpleBuildItem {

    private final Set<DotName> annotationTypes;
    private final List<ListenerDeclaration> listeners;
    private final List<ListenerDeclaration> routedObservers;

    /**
     * @param annotationTypes {@code @ApplicationModuleListener} and the annotations composed from it
     * @param listeners the {@code @ApplicationModuleListener} methods
     * @param routedObservers the observers the dispatcher delivers to instead of ArC
     */
    public ListenerDeclarationsBuildItem(Set<DotName> annotationTypes, List<ListenerDeclaration> listeners,
            List<ListenerDeclaration> routedObservers) {
        this.annotationTypes = Collections.unmodifiableSet(new LinkedHashSet<>(annotationTypes));
        this.listeners = List.copyOf(listeners);
        this.routedObservers = List.copyOf(routedObservers);
    }

    public Set<DotName> getAnnotationTypes() {
        return annotationTypes;
    }

    public List<ListenerDeclaration> getRoutedObservers() {
        return routedObservers;
    }

    /**
     * @return the listener methods first, then the routed observers
     */
    public List<ListenerDeclaration> getAll() {
        return Stream.concat(listeners.stream(), routedObservers.stream()).toList();
    }

    public boolean isEmpty() {
        return listeners.isEmpty() && routedObservers.isEmpty();
    }
}
