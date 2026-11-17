package io.quarkiverse.modulith.core.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Walks the application part of a type hierarchy.
 */
final class Hierarchy {

    private Hierarchy() {
    }

    /**
     * @return the class and its superclasses and interfaces that are application classes, the class first
     */
    static Stream<ScannedClass> of(TypePool pool, ScannedClass type) {
        Set<String> seen = new HashSet<>();
        Deque<ScannedClass> queue = new ArrayDeque<>(List.of(type));
        List<ScannedClass> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            ScannedClass current = queue.pop();
            if (seen.add(current.name())) {
                result.add(current);
                Stream.concat(Stream.of(current.superName()), current.interfaces().stream())
                        .flatMap(it -> pool.application(it).stream())
                        .forEach(queue::add);
            }
        }
        return result.stream();
    }
}
