package io.quarkiverse.modulith.docs;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import io.quarkiverse.modulith.core.ApplicationModule;

/**
 * The diagram element of a module, passed to {@link DiagramOptions#withComponentFilter(java.util.function.Predicate)}.
 * Plays the role of the Structurizr {@code Component} in Spring Modulith.
 */
public final class DiagramComponent {

    private final int id;
    private final String name;
    private final ApplicationModule module;
    private final Set<String> tags = new LinkedHashSet<>(Set.of("Element"));

    DiagramComponent(int id, String name, ApplicationModule module) {
        this.id = id;
        this.name = name;
        this.module = module;
        tags.add("Component");
    }

    int id() {
        return id;
    }

    /**
     * @return the label of the component, the module's display name by default
     */
    public String getName() {
        return name;
    }

    /**
     * @return the module the component stands for
     */
    public ApplicationModule getModule() {
        return module;
    }

    /**
     * @return the tags of the component, {@code Element}, {@code Component} and one tag per background color
     */
    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    void addTag(String tag) {
        tags.add(tag);
    }

    @Override
    public String toString() {
        return "DiagramComponent[" + name + "]";
    }
}
