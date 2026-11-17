package io.quarkiverse.modulith.core.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.objectweb.asm.Opcodes;

import io.quarkiverse.modulith.core.ArchitecturallyEvidentType;
import io.quarkiverse.modulith.core.EntryPointKind;
import io.quarkiverse.modulith.core.data.AnnotationData;
import io.quarkiverse.modulith.core.data.EntryPointData;
import io.quarkiverse.modulith.core.data.ReferenceMethodData;
import io.quarkiverse.modulith.core.data.TypeData;

/**
 * Classifies scanned classes: beans, architectural stereotypes, event listener methods and entry points. Spring's
 * stereotypes are recognized next to the CDI, Jakarta, Quarkus and jMolecules ones, so that the same code base gives
 * the same model under Spring Modulith and Quarkus Modulith.
 */
final class TypeFacts {

    private final TypePool pool;

    TypeFacts(TypePool pool) {
        this.pool = pool;
    }

    /**
     * @return whether the class declares a bean on its own: a scoped, stereotyped or Spring component class, a REST
     *         resource, a repository interface, a configuration mapping, a validator or a Panache repository
     */
    boolean isBean(ScannedClass type) {
        boolean annotationType = (type.access() & Opcodes.ACC_ANNOTATION) != 0;
        boolean component = !type.isInterface() && pool.isMetaAnnotated(type.annotations(), Names.SCOPES);
        boolean repository = type.isInterface() && (isAssignableTo(type, Names.SPRING_DATA_REPOSITORY)
                || pool.isMetaAnnotated(type.annotations(), Names.REPOSITORY_DEFINITION));
        boolean other = pool.isMetaAnnotated(type.annotations(), Names.CONFIGURATION_PROPERTIES)
                || isAssignableTo(type, Set.of(Names.CONSTRAINT_VALIDATOR))
                || !type.isInterface() && isAssignableTo(type, Names.PANACHE_REPOSITORY);
        return !annotationType && (component || repository || other);
    }

    /**
     * @return the application types produced by the class's producer methods and fields
     */
    Stream<String> producedTypes(ScannedClass type) {
        return Stream.concat(
                type.methods().stream().filter(it -> it.hasAnnotation(Names.PRODUCER)).map(ScannedMethod::returnType),
                type.fields().stream().filter(it -> it.annotations().stream().anyMatch(a -> Names.PRODUCER.contains(a.type())))
                        .map(ScannedField::type))
                .filter(pool::isApplicationClass);
    }

    boolean isAssignableTo(ScannedClass type, Set<String> targets) {
        return targets.stream().anyMatch(it -> pool.isAssignableTo(type.name(), it));
    }

    boolean isConfiguration(String type) {
        return pool.get(type).filter(it -> pool.isMetaAnnotated(it.annotations(), Names.CONFIGURATION)).isPresent();
    }

    boolean isEntity(ScannedClass type) {
        return pool.isMetaAnnotated(type.annotations(), Names.ENTITY) || isAssignableTo(type, Names.ENTITY_TYPE)
                || isAssignableTo(type, Names.PANACHE_ENTITY);
    }

    /**
     * @return the type data with the stereotypes and event listener methods of the class, in a module with the given
     *         beans and repositories
     */
    TypeData classified(TypeData data, ScannedClass type, Collection<String> beans,
            Collection<ScannedClass> repositories) {
        List<ReferenceMethodData> referenceMethods = referenceMethods(type);
        return data.withStereotypes(stereotypes(type, beans, repositories, !referenceMethods.isEmpty()),
                referenceMethods);
    }

    private List<String> stereotypes(ScannedClass type, Collection<String> beans, Collection<ScannedClass> repositories,
            boolean listener) {
        List<String> result = new ArrayList<>();
        boolean entity = isEntity(type);
        boolean repository = pool.isMetaAnnotated(type.annotations(), Names.REPOSITORY)
                || isAssignableTo(type, Names.REPOSITORY_TYPE);
        boolean service = pool.isMetaAnnotated(type.annotations(), Names.SERVICE);
        boolean controller = pool.isMetaAnnotated(type.annotations(), Names.CONTROLLER)
                || type.methods().stream().anyMatch(it -> pool.isMetaAnnotated(it.annotations(), Names.ROUTE));
        boolean configuration = pool.isMetaAnnotated(type.annotations(), Names.CONFIGURATION_PROPERTIES);
        boolean aggregateRoot = pool.isMetaAnnotated(type.annotations(), Names.AGGREGATE_ROOT)
                || isAssignableTo(type, Names.AGGREGATE_ROOT_TYPE)
                || entity && (isAssignableTo(type, Names.PANACHE_ENTITY) || repositories.stream()
                        .anyMatch(it -> domainTypes(it).anyMatch(domain -> pool.isAssignableTo(type.name(), domain))));
        add(result, entity, ArchitecturallyEvidentType.ENTITY);
        add(result, aggregateRoot, ArchitecturallyEvidentType.AGGREGATE_ROOT);
        add(result, repository, ArchitecturallyEvidentType.REPOSITORY);
        add(result, service, ArchitecturallyEvidentType.SERVICE);
        add(result, controller, ArchitecturallyEvidentType.CONTROLLER);
        add(result, listener, ArchitecturallyEvidentType.EVENT_LISTENER);
        add(result, configuration, ArchitecturallyEvidentType.CONFIGURATION_PROPERTIES);
        add(result, beans.contains(type.name()) || pool.isMetaAnnotated(type.annotations(), Names.SCOPES) || service
                || controller || listener, ArchitecturallyEvidentType.INJECTABLE);
        add(result, pool.isMetaAnnotated(type.annotations(), Names.VALUE_OBJECT)
                || isAssignableTo(type, Names.VALUE_OBJECT_TYPE), ArchitecturallyEvidentType.VALUE_OBJECT);
        return result;
    }

    private static void add(List<String> result, boolean condition, String stereotype) {
        if (condition) {
            result.add(stereotype);
        }
    }

    /**
     * @return the domain types of a repository: the first type argument of its generic supertypes
     */
    Stream<String> domainTypes(ScannedClass repository) {
        return repository.typeArguments().stream();
    }

    /**
     * @return whether the method receives events
     */
    boolean isEventListener(ScannedClass owner, ScannedMethod method) {
        return isListenerMethod(method) || method.name().equals("onApplicationEvent") && !method.isSynthetic()
                && pool.isAssignableTo(owner.name(), Names.APPLICATION_LISTENER);
    }

    /**
     * @return whether the method is declared as listener, which makes its parameters event listener dependencies
     */
    boolean isListenerMethod(ScannedMethod method) {
        return pool.isMetaAnnotated(method.annotations(), Names.SPRING_EVENT_LISTENER)
                || pool.isMetaAnnotated(method.annotations(), Names.DOMAIN_EVENT_HANDLER)
                || method.hasAnnotation(Names.MODULE_LISTENER)
                || method.hasParameterAnnotation(Names.OBSERVES);
    }

    /**
     * @return the event listener methods, sorted by name and parameter count
     */
    List<ReferenceMethodData> referenceMethods(ScannedClass type) {
        return type.methods().stream()
                .filter(it -> isEventListener(type, it))
                .sorted(Comparator.comparing(ScannedMethod::name).thenComparing(it -> it.parameterTypes().size()))
                .map(it -> new ReferenceMethodData(it.name(), it.parameterTypes(), isAsync(it),
                        it.annotation(Set.of(Names.TRANSACTIONAL_EVENT_LISTENER))
                                .map(a -> a.value("phase").orElse("AFTER_COMMIT")).orElse(""),
                        referenceTypes(it)))
                .toList();
    }

    private boolean isAsync(ScannedMethod method) {
        return pool.isMetaAnnotated(method.annotations(), Names.SPRING_ASYNC) || method.hasAnnotation(Names.MODULE_LISTENER)
                || method.hasParameterAnnotation(Set.of(Names.OBSERVES_ASYNC));
    }

    private List<String> referenceTypes(ScannedMethod method) {
        return method.parameterTypes().isEmpty()
                ? pool.declaration(method.annotations(), Names.SPRING_EVENT_LISTENER)
                        .flatMap(it -> it.values("classes")).orElse(List.of())
                : method.parameterTypes();
    }

    /**
     * @return the methods the framework calls: REST resources and routes, listeners, consumers and scheduled methods
     */
    List<EntryPointData> entryPoints(ScannedClass type) {
        Optional<String> classPath = annotation(type.annotations(), Set.of(Names.PATH)).flatMap(it -> it.value("value"));
        return type.methods().stream().flatMap(method -> Stream.of(
                httpEntryPoint(type, method, classPath),
                isEventListener(type, method)
                        ? Optional.of(entryPoint(type, method, EntryPointKind.EVENT_LISTENER,
                                String.join(", ", referenceTypes(method))))
                        : Optional.<EntryPointData> empty(),
                pool.declaration(method.annotations(), Names.INCOMING)
                        .map(it -> entryPoint(type, method, EntryPointKind.MESSAGING, describe(it))),
                pool.declaration(method.annotations(), Names.SCHEDULED)
                        .map(it -> entryPoint(type, method, EntryPointKind.SCHEDULED, describe(it))))
                .flatMap(Optional::stream))
                .toList();
    }

    private Optional<EntryPointData> httpEntryPoint(ScannedClass type, ScannedMethod method, Optional<String> classPath) {
        Optional<String> verb = method.annotations().stream()
                .filter(it -> pool.metaAnnotations(List.of(it)).contains(Names.HTTP_METHOD))
                .map(it -> it.type().substring(it.type().lastIndexOf('.') + 1))
                .findFirst();
        Optional<String> path = annotation(method.annotations(), Set.of(Names.PATH)).flatMap(it -> it.value("value"));
        Optional<EntryPointData> rest = verb.or(() -> path.filter(it -> classPath.isPresent()).map(it -> "*"))
                .map(it -> entryPoint(type, method, EntryPointKind.HTTP,
                        (it + " " + join(classPath.orElse(""), path.orElse(""))).trim()));
        return rest.or(() -> pool.declaration(method.annotations(), Names.ROUTE)
                .or(() -> pool.declaration(method.annotations(), Names.REQUEST_MAPPING))
                .map(it -> entryPoint(type, method, EntryPointKind.HTTP, describe(it))));
    }

    private static String join(String classPath, String methodPath) {
        String left = classPath.endsWith("/") ? classPath.substring(0, classPath.length() - 1) : classPath;
        String right = methodPath.isEmpty() || methodPath.startsWith("/") ? methodPath : "/" + methodPath;
        return left + right;
    }

    private static String describe(AnnotationData annotation) {
        return annotation.attributes().stream().map(it -> it.name() + "=" + String.join(",", it.values()))
                .reduce((left, right) -> left + " " + right).orElse("");
    }

    private static EntryPointData entryPoint(ScannedClass type, ScannedMethod method, EntryPointKind kind, String detail) {
        return new EntryPointData(type.name(), method.name(), kind.name(), detail);
    }

    private static Optional<AnnotationData> annotation(List<AnnotationData> annotations, Set<String> types) {
        return annotations.stream().filter(it -> types.contains(it.type())).findFirst();
    }
}
