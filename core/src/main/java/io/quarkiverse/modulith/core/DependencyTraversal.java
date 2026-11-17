package io.quarkiverse.modulith.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The depth, the dependency types and the modules seen so far while collecting dependencies.
 */
final class DependencyTraversal {

    private final Set<String> seen = new HashSet<>();
    private final DependencyDepth depth;
    private final Set<DependencyType> types;

    DependencyTraversal(DependencyDepth depth, DependencyType... types) {
        this.depth = depth;
        this.types = Set.copyOf(List.of(types));
    }

    boolean include(DependencyType type) {
        return types.isEmpty() || types.contains(type);
    }

    boolean isTransitive() {
        return depth == DependencyDepth.ALL;
    }

    /**
     * @return whether the module was not seen before
     */
    boolean register(ApplicationModule module) {
        return seen.add(module.getName());
    }
}
