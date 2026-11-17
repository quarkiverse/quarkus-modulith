package io.quarkiverse.modulith.events;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Finds the externalization annotations of an event type, like Spring Modulith's {@code AnnotationTargetLookup}:
 * Quarkus Modulith's {@link Externalized} and jMolecules' {@code org.jmolecules.event.annotation.Externalized}. The
 * annotations are searched on the type, its superclasses and interfaces, and as meta-annotations. Results are cached
 * per type.
 */
final class AnnotationTargetLookup {

    static final String JMOLECULES_EXTERNALIZED = "org.jmolecules.event.annotation.Externalized";

    private static final ExternalizedTargets TARGETS = new ExternalizedTargets();

    private AnnotationTargetLookup() {
    }

    /**
     * @return whether the type of the event carries a supported {@code Externalized} annotation
     */
    static boolean hasExternalizedAnnotation(Object event) {
        return find(Objects.requireNonNull(event, "event").getClass()).findAny().isPresent();
    }

    /**
     * @return the routing target declared by the first supported annotation with a non-blank target, jMolecules first
     */
    static Optional<ParsedRoutingTarget> targetOf(Class<?> type) {
        return TARGETS.get(type);
    }

    static Optional<ParsedRoutingTarget> lookup(Class<?> type) {
        List<Annotation> found = find(type).toList();
        return Stream.concat(found.stream().filter(AnnotationTargetLookup::isJMolecules),
                found.stream().filter(Predicate.not(AnnotationTargetLookup::isJMolecules)))
                .flatMap(AnnotationTargetLookup::targetAndValue)
                .filter(value -> !value.isBlank())
                .findFirst()
                .map(ParsedRoutingTarget::parse);
    }

    /**
     * @return the supported annotations on the type, its supertypes and as meta-annotations, nearest first
     */
    static Stream<Annotation> find(Class<?> type) {
        return AnnotationLookup.findAll(type, AnnotationTargetLookup::isSupported);
    }

    private static Stream<String> targetAndValue(Annotation annotation) {
        return annotation instanceof Externalized externalized
                ? Stream.of(externalized.target(), externalized.value())
                : Stream.of(attribute(annotation, "target"), attribute(annotation, "value"));
    }

    private static boolean isSupported(Annotation annotation) {
        return annotation instanceof Externalized || isJMolecules(annotation);
    }

    private static boolean isJMolecules(Annotation annotation) {
        return annotation.annotationType().getName().equals(JMOLECULES_EXTERNALIZED);
    }

    /**
     * Reads a {@code String} attribute of an annotation the API cannot compile against.
     *
     * @return the value, or an empty string when the annotation has no such attribute
     */
    static String attribute(Annotation annotation, String name) {
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                try {
                    return String.valueOf(method.invoke(annotation));
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot read attribute " + name + " of "
                            + annotation.annotationType().getName(), e);
                }
            }
        }
        return "";
    }
}
