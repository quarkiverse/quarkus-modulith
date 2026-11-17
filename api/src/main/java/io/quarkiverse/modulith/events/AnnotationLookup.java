package io.quarkiverse.modulith.events;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Finds annotations on a type, its superclasses and interfaces, and as meta-annotations, nearest first, like Spring's
 * {@code AnnotatedElementUtils} find semantics.
 */
final class AnnotationLookup {

    private static final String JAVA_LANG_ANNOTATION = "java.lang.annotation.";

    private AnnotationLookup() {
    }

    /**
     * @return the first annotation of the given type
     */
    static <T extends Annotation> Optional<T> find(Class<?> type, Class<T> annotation) {
        return findAll(type, annotation::isInstance).findFirst().map(annotation::cast);
    }

    /**
     * @return the annotations matching the predicate, nearest first
     */
    static Stream<Annotation> findAll(Class<?> type, Predicate<Annotation> matches) {
        Stream.Builder<Annotation> found = Stream.builder();
        Set<Class<?>> visited = new HashSet<>();
        Deque<Class<?>> pending = new ArrayDeque<>();
        pending.add(type);
        while (!pending.isEmpty()) {
            Class<?> current = pending.poll();
            if (!visited.add(current)) {
                continue;
            }
            for (Annotation annotation : current.getDeclaredAnnotations()) {
                if (matches.test(annotation)) {
                    found.add(annotation);
                } else if (!annotation.annotationType().getName().startsWith(JAVA_LANG_ANNOTATION)) {
                    pending.add(annotation.annotationType());
                }
            }
            if (Objects.nonNull(current.getSuperclass())) {
                pending.add(current.getSuperclass());
            }
            pending.addAll(List.of(current.getInterfaces()));
        }
        return found.build();
    }
}
