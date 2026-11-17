package io.quarkiverse.modulith.observability.runtime;

import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Orders types most specific first, like Spring Modulith's {@code ModulithMetricsCustomizer}: by the depth of the type
 * in its hierarchy, deepest first, then by name. A subtype is always deeper than each of its supertypes, so the order
 * puts subtypes before their supertypes and stays consistent (transitive, {@code 0} only for the same name) with
 * interfaces too.
 */
final class TypeSpecificity {

    private static final Comparator<Class<?>> ORDER = Comparator.<Class<?>> comparingInt(TypeSpecificity::depth)
            .reversed()
            .thenComparing(Class::getName);

    private TypeSpecificity() {
    }

    static int compare(Class<?> first, Class<?> second) {
        return ORDER.compare(first, second);
    }

    /**
     * @return the length of the longest path from the type to a root of its hierarchy, over superclasses and
     *         interfaces: {@code 0} for {@code Object} and for interfaces without superinterfaces
     */
    static int depth(Class<?> type) {
        return Stream.concat(Stream.ofNullable(type.getSuperclass()), Stream.of(type.getInterfaces()))
                .mapToInt(it -> depth(it) + 1)
                .max()
                .orElse(0);
    }
}
