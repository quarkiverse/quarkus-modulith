package io.quarkiverse.modulith.core.impl;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import io.quarkiverse.modulith.core.data.AnnotationData;

/**
 * The scanned application classes, plus library classes read on demand to resolve supertypes and meta-annotations.
 */
final class TypePool {

    private final Map<String, ScannedClass> application;
    private final Function<String, Optional<byte[]>> library;
    private final Map<String, Optional<ScannedClass>> resolved = new HashMap<>();
    private final Map<String, Set<String>> supertypes = new HashMap<>();
    private final Map<String, Set<String>> metaAnnotations = new HashMap<>();

    TypePool(Map<String, ScannedClass> application, Function<String, Optional<byte[]>> library) {
        this.application = application;
        this.library = library;
    }

    Collection<ScannedClass> application() {
        return application.values();
    }

    boolean isApplicationClass(String name) {
        return application.containsKey(name);
    }

    Optional<ScannedClass> application(String name) {
        return Optional.ofNullable(application.get(name));
    }

    /**
     * @return the class, from the application or read from the class path
     */
    Optional<ScannedClass> get(String name) {
        return application(name).or(() -> resolved.computeIfAbsent(name, it -> library.apply(it).map(ClassFileScanner::scan)));
    }

    /**
     * @return all supertypes of the class, transitively, {@code java.lang.Object} excluded
     */
    Set<String> supertypes(String name) {
        return supertypes.computeIfAbsent(name, this::collectSupertypes);
    }

    private Set<String> collectSupertypes(String name) {
        Set<String> result = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>(List.of(name));
        while (!queue.isEmpty()) {
            get(queue.pop()).ifPresent(type -> Stream.concat(Stream.of(type.superName()), type.interfaces().stream())
                    .filter(it -> !it.isEmpty() && !it.equals("java.lang.Object"))
                    .filter(result::add)
                    .forEach(queue::add));
        }
        return result;
    }

    boolean isAssignableTo(String name, String type) {
        return name.equals(type) || supertypes(name).contains(type);
    }

    /**
     * @return the types of the annotations present on the given annotations, transitively
     */
    Set<String> metaAnnotations(List<AnnotationData> annotations) {
        Set<String> result = new LinkedHashSet<>();
        annotations.forEach(it -> result.addAll(metaAnnotationsOf(it.type())));
        return result;
    }

    private Set<String> metaAnnotationsOf(String annotation) {
        return metaAnnotations.computeIfAbsent(annotation, this::collectMetaAnnotations);
    }

    private Set<String> collectMetaAnnotations(String annotation) {
        Set<String> result = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>(List.of(annotation));
        while (!queue.isEmpty()) {
            get(queue.pop()).ifPresent(type -> type.annotations().stream()
                    .map(AnnotationData::type)
                    .filter(it -> !it.startsWith("java.lang.annotation."))
                    .filter(result::add)
                    .forEach(queue::add));
        }
        return result;
    }

    /**
     * @return whether one of the annotations is of the given type or is (meta-)annotated with it
     */
    boolean isMetaAnnotated(List<AnnotationData> annotations, Set<String> types) {
        return annotations.stream().anyMatch(it -> types.contains(it.type()))
                || metaAnnotations(annotations).stream().anyMatch(types::contains);
    }

    /**
     * @return the declaration of one of the given annotation types: on the element itself, or on one of its
     *         annotations
     */
    Optional<AnnotationData> declaration(List<AnnotationData> annotations, Set<String> types) {
        return annotations.stream().filter(it -> types.contains(it.type())).findFirst()
                .or(() -> annotations.stream().flatMap(it -> get(it.type()).stream())
                        .flatMap(it -> it.annotations().stream())
                        .filter(it -> types.contains(it.type()))
                        .findFirst());
    }
}
