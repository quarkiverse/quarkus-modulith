package io.quarkiverse.modulith.deployment.docs;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 * Answers questions about types from the Jandex index: annotations, meta-annotations, supertypes and type names.
 */
final class TypeInspector {

    private final IndexView index;

    TypeInspector(IndexView index) {
        this.index = index;
    }

    Optional<ClassInfo> classInfo(String name) {
        return Optional.ofNullable(index.getClassByName(DotName.createSimple(name)));
    }

    /**
     * @return the names of the annotations declared on the class
     */
    Set<String> annotations(ClassInfo type) {
        return type.declaredAnnotations().stream().map(it -> it.name().toString())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * @return the names of the annotations found on the given annotations, transitively, as far as the index knows
     *         them
     */
    Set<String> metaAnnotations(Set<String> annotations) {
        Set<String> result = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>(annotations);
        while (!queue.isEmpty()) {
            classInfo(queue.pop()).ifPresent(annotation -> annotations(annotation).stream()
                    .filter(result::add)
                    .forEach(queue::add));
        }
        return result;
    }

    /**
     * @return the annotations of the type's package and their meta-annotations
     */
    Set<String> packageAnnotations(String type) {
        String packageName = packageOf(type);
        Set<String> result = new LinkedHashSet<>(classInfo(packageName + ".package-info")
                .map(this::annotations).orElse(Set.of()));
        result.addAll(metaAnnotations(result));
        return result;
    }

    /**
     * @return every superclass and interface of the type, transitively; names missing from the index end the walk
     */
    Set<String> supertypes(ClassInfo type) {
        Set<String> result = new LinkedHashSet<>();
        Deque<ClassInfo> queue = new ArrayDeque<>(List.of(type));
        while (!queue.isEmpty()) {
            ClassInfo current = queue.pop();
            Set<DotName> parents = new LinkedHashSet<>();
            if (Objects.nonNull(current.superName())) {
                parents.add(current.superName());
            }
            parents.addAll(current.interfaceNames());
            for (DotName parent : parents) {
                if (result.add(parent.toString())) {
                    classInfo(parent.toString()).ifPresent(queue::add);
                }
            }
        }
        return result;
    }

    /**
     * @return whether one of the methods the class declares carries the annotation
     */
    static boolean hasMethodAnnotation(ClassInfo type, DotName annotation) {
        return type.methods().stream().anyMatch(it -> it.hasDeclaredAnnotation(annotation));
    }

    static boolean hasParameterAnnotation(MethodInfo method, DotName annotation) {
        return method.parameters().stream().anyMatch(it -> it.hasDeclaredAnnotation(annotation));
    }

    static boolean hasAnnotation(ClassInfo type, DotName annotation) {
        return Objects.nonNull(type.declaredAnnotation(annotation));
    }

    static Optional<AnnotationInstance> annotation(ClassInfo type, DotName annotation) {
        return Optional.ofNullable(type.declaredAnnotation(annotation));
    }

    /**
     * @return the binary name of the erased type, arrays written as {@code component[]}
     */
    static String typeName(Type type) {
        if (type.kind() == Type.Kind.ARRAY) {
            return typeName(type.asArrayType().elementType()) + "[]".repeat(type.asArrayType().deepDimensions());
        }
        return type.name().toString();
    }

    static String packageOf(String type) {
        int dot = type.lastIndexOf('.');
        return dot < 0 ? "" : type.substring(0, dot);
    }
}
