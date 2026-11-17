package io.quarkiverse.modulith.observability.deployment;

import java.util.List;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The bean classes and resource interfaces whose calls are observed, and the encoded observed methods.
 */
public final class ObservedTypesBuildItem extends SimpleBuildItem {

    private final Set<String> classes;
    private final Set<String> resources;
    private final List<String> rows;

    public ObservedTypesBuildItem(Set<String> classes, Set<String> resources, List<String> rows) {
        this.classes = Set.copyOf(classes);
        this.resources = Set.copyOf(resources);
        this.rows = List.copyOf(rows);
    }

    /**
     * @return the REST Data with Panache resource interfaces whose generated implementations are observed
     */
    public Set<String> getResources() {
        return resources;
    }

    public Set<String> getClasses() {
        return classes;
    }

    public List<String> getRows() {
        return rows;
    }
}
