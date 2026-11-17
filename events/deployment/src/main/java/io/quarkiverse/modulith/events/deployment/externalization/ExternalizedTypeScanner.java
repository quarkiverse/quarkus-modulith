package io.quarkiverse.modulith.events.deployment.externalization;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkiverse.modulith.events.Externalized;

/**
 * Finds the event types that carry a supported {@code Externalized} annotation, Quarkus Modulith's or jMolecules', with
 * the same search the runtime uses: on the type, its superclasses and interfaces, and as meta-annotations.
 */
final class ExternalizedTypeScanner {

    static final DotName EXTERNALIZED = DotName.createSimple(Externalized.class);
    static final DotName JMOLECULES_EXTERNALIZED = DotName.createSimple("org.jmolecules.event.annotation.Externalized");
    private static final String JAVA_LANG_ANNOTATION = "java.lang.annotation.";

    private final IndexView index;
    private final IndexView computing;
    private final Set<DotName> annotationTypes = new TreeSet<>();

    /**
     * @param index the application index, searched for annotated types
     * @param computing an index that also knows the JDK and library classes, used to walk type hierarchies
     */
    ExternalizedTypeScanner(IndexView index, IndexView computing) {
        this.index = index;
        this.computing = computing;
    }

    /**
     * @return the concrete externalized types with every supported annotation found on them, nearest first, sorted by
     *         type name
     */
    Map<DotName, List<AnnotationInstance>> scan() {
        Set<ClassInfo> annotated = new LinkedHashSet<>();
        Set<DotName> visitedAnnotations = new HashSet<>();
        Deque<DotName> annotations = new ArrayDeque<>(List.of(EXTERNALIZED, JMOLECULES_EXTERNALIZED));
        while (!annotations.isEmpty()) {
            DotName annotation = annotations.poll();
            if (!visitedAnnotations.add(annotation)) {
                continue;
            }
            if (onClassPath(annotation) && Objects.nonNull(computing.getClassByName(annotation))) {
                annotationTypes.add(annotation);
            }
            for (AnnotationInstance instance : index.getAnnotations(annotation)) {
                ClassInfo type = instance.target().asClass();
                if (type.isAnnotation()) {
                    annotations.add(type.name());
                } else {
                    annotated.add(type);
                }
            }
        }
        Map<DotName, List<AnnotationInstance>> result = new TreeMap<>();
        for (ClassInfo type : annotated) {
            Stream.concat(Stream.of(type), Stream.concat(index.getAllKnownSubclasses(type.name()).stream(),
                    index.getAllKnownImplementations(type.name()).stream()))
                    .filter(ExternalizedTypeScanner::isConcrete)
                    .forEach(concrete -> result.computeIfAbsent(concrete.name(), name -> annotationsOf(concrete)));
        }
        return result;
    }

    /**
     * @return the supported annotations on the class path and the annotations meta-annotated with them, known after
     *         {@link #scan()}
     */
    Set<DotName> annotationTypes() {
        return annotationTypes;
    }

    /**
     * @return the supported annotations on the type, its supertypes and as meta-annotations, nearest first
     */
    List<AnnotationInstance> annotationsOf(ClassInfo type) {
        List<AnnotationInstance> found = new ArrayList<>();
        Set<DotName> visited = new HashSet<>();
        Deque<DotName> pending = new ArrayDeque<>(List.of(type.name()));
        while (!pending.isEmpty()) {
            ClassInfo current = computing.getClassByName(pending.poll());
            if (Objects.isNull(current) || !visited.add(current.name())) {
                continue;
            }
            for (AnnotationInstance annotation : current.declaredAnnotations()) {
                if (isSupported(annotation.name())) {
                    found.add(annotation);
                } else if (!annotation.name().toString().startsWith(JAVA_LANG_ANNOTATION)) {
                    pending.add(annotation.name());
                }
            }
            Optional.ofNullable(current.superName()).ifPresent(pending::add);
            pending.addAll(current.interfaceNames());
        }
        return found;
    }

    /**
     * @return the target the runtime takes from the annotations: the first non-blank {@code target} or {@code value},
     *         jMolecules' annotation first, empty when none is set
     */
    static String declaration(List<AnnotationInstance> annotations) {
        Predicate<AnnotationInstance> jMolecules = annotation -> annotation.name().equals(JMOLECULES_EXTERNALIZED);
        return Stream.concat(annotations.stream().filter(jMolecules), annotations.stream().filter(jMolecules.negate()))
                .flatMap(annotation -> Stream.of(attribute(annotation, "target"), attribute(annotation, "value")))
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse("");
    }

    /**
     * @return every non-blank {@code target} and {@code value} of the annotations
     */
    static Set<String> declarations(List<AnnotationInstance> annotations) {
        Set<String> result = new LinkedHashSet<>();
        for (AnnotationInstance annotation : annotations) {
            Stream.of(attribute(annotation, "target"), attribute(annotation, "value"))
                    .filter(value -> !value.isBlank())
                    .forEach(result::add);
        }
        return result;
    }

    private static String attribute(AnnotationInstance annotation, String name) {
        return Optional.ofNullable(annotation.value(name)).map(AnnotationValue::asString).orElse("");
    }

    private static boolean isSupported(DotName annotation) {
        return annotation.equals(EXTERNALIZED) || annotation.equals(JMOLECULES_EXTERNALIZED);
    }

    /**
     * Checks for the class file before the computing index is asked for the class, because the computing index logs a
     * warning for every class it cannot find, and jMolecules is optional.
     */
    private static boolean onClassPath(DotName type) {
        return Objects.nonNull(Thread.currentThread().getContextClassLoader()
                .getResource(type.toString().replace('.', '/') + ".class"));
    }

    private static boolean isConcrete(ClassInfo type) {
        return !type.isInterface() && !type.isAbstract();
    }
}
