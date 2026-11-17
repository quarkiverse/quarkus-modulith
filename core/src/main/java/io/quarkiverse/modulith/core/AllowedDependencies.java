package io.quarkiverse.modulith.core;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The allowed dependencies of a module, explicitly declared or granted as shared modules. Like Spring Modulith's
 * {@code ApplicationModule.AllowedDependencies}.
 */
public final class AllowedDependencies implements Iterable<AllowedDependency> {

    private final List<AllowedDependency> dependencies;
    private final boolean closed;

    private AllowedDependencies(List<AllowedDependency> dependencies, boolean closed) {
        this.dependencies = dependencies;
        this.closed = closed;
    }

    static AllowedDependencies open() {
        return new AllowedDependencies(List.of(), false);
    }

    static AllowedDependencies closed(List<AllowedDependency> dependencies) {
        return new AllowedDependencies(dependencies, true);
    }

    /**
     * @return whether no dependency is listed, which means none is allowed for a module that declares its allowed
     *         dependencies, and any for one that does not
     */
    public boolean isEmpty() {
        return dependencies.isEmpty();
    }

    /**
     * @return whether the module declares its allowed dependencies
     */
    public boolean isClosed() {
        return closed;
    }

    public Stream<AllowedDependency> stream() {
        return dependencies.stream();
    }

    /**
     * @return whether the given type may be referenced
     */
    boolean isAllowedDependency(JavaType type) {
        return !closed || dependencies.stream().anyMatch(it -> it.contains(type));
    }

    @Override
    public Iterator<AllowedDependency> iterator() {
        return dependencies.iterator();
    }

    @Override
    public String toString() {
        return dependencies.isEmpty() ? "none"
                : dependencies.stream().map(AllowedDependency::toString).collect(Collectors.joining(", "));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AllowedDependencies that && dependencies.equals(that.dependencies);
    }

    @Override
    public int hashCode() {
        return dependencies.hashCode();
    }
}
