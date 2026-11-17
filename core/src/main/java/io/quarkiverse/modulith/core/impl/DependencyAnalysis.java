package io.quarkiverse.modulith.core.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.Type;

import io.quarkiverse.modulith.core.DependencyType;
import io.quarkiverse.modulith.core.data.AnnotationData;
import io.quarkiverse.modulith.core.data.DependencyData;

/**
 * Computes the references from the classes of a module to the classes of other modules, with the kinds and
 * descriptions Spring Modulith gives them: injection points of beans are {@link DependencyType#USES_COMPONENT},
 * listener method parameters {@link DependencyType#EVENT_LISTENER}, references to JPA entities
 * {@link DependencyType#ENTITY}, the rest {@link DependencyType#DEFAULT}.
 */
final class DependencyAnalysis {

    private final TypePool pool;
    private final TypeFacts facts;
    private final Map<String, String> moduleOfType;
    private final boolean allowFieldInjection;

    /**
     * @param moduleOfType the identifier of the module of each module type
     */
    DependencyAnalysis(TypePool pool, TypeFacts facts, Map<String, String> moduleOfType, boolean allowFieldInjection) {
        this.pool = pool;
        this.facts = facts;
        this.moduleOfType = moduleOfType;
        this.allowFieldInjection = allowFieldInjection;
    }

    /**
     * @param module the identifier of the module
     * @param type a class of the module
     * @param injectable whether the class is a bean other beans may depend on
     * @param configurationProperties whether the class holds configuration
     */
    Stream<DependencyData> dependencies(String module, ScannedClass type, boolean injectable,
            boolean configurationProperties) {
        Stream<DependencyData> injections = Stream.of(constructorDependencies(type, injectable && !configurationProperties),
                methodDependencies(type), fieldDependencies(type)).flatMap(it -> it);
        Stream<DependencyData> direct = type.dependencies().stream()
                .map(it -> new DependencyData(type.name(), it.target(), typeOf(it.target()).name(), it.description(), ""));
        return Stream.concat(injections, direct)
                .filter(it -> moduleOfType.containsKey(it.target()) && !moduleOfType.get(it.target()).equals(module));
    }

    private Stream<DependencyData> constructorDependencies(ScannedClass type, boolean injection) {
        List<ScannedMethod> constructors = type.methods().stream().filter(ScannedMethod::isConstructor).toList();
        return constructors.stream()
                .filter(it -> constructors.size() == 1 || it.hasAnnotation(Names.INJECT))
                .flatMap(constructor -> classParameters(constructor).map(parameter -> injection
                        ? new DependencyData(type.name(), parameter, DependencyType.USES_COMPONENT.name(),
                                describe(constructor, type.name(), type, "constructor"), "")
                        : new DependencyData(type.name(), parameter, DependencyType.DEFAULT.name(),
                                describe(constructor, parameter, type, "parameter"), "")));
    }

    private Stream<DependencyData> methodDependencies(ScannedClass type) {
        return Hierarchy.of(pool, type).flatMap(owner -> owner.methods().stream()
                .filter(it -> !it.name().startsWith("<"))
                .flatMap(method -> Stream.concat(returnDependency(owner, method), parameterDependencies(owner, method))));
    }

    private Stream<DependencyData> returnDependency(ScannedClass owner, ScannedMethod method) {
        return Stream.of(method.returnType()).filter(DependencyAnalysis::isClass)
                .map(it -> new DependencyData(owner.name(), it, DependencyType.DEFAULT.name(),
                        describe(method, it, owner, "return type"), ""));
    }

    private Stream<DependencyData> parameterDependencies(ScannedClass owner, ScannedMethod method) {
        if (method.hasAnnotation(Names.INJECT)) {
            return classParameters(method).map(it -> new DependencyData(owner.name(), it,
                    DependencyType.USES_COMPONENT.name(), describe(method, owner.name(), owner, "injection method"), ""));
        }
        boolean listener = facts.isListenerMethod(method);
        return classParameters(method).map(it -> new DependencyData(owner.name(), it,
                (listener ? DependencyType.EVENT_LISTENER : typeOf(it)).name(), describe(method, it, owner, "parameter"),
                ""));
    }

    private Stream<DependencyData> fieldDependencies(ScannedClass type) {
        return superclasses(type).flatMap(owner -> owner.fields().stream()
                .filter(it -> isClass(it.type()))
                .filter(it -> it.annotations().stream().anyMatch(annotation -> Names.INJECT.contains(annotation.type())))
                .map(field -> new DependencyData(owner.name(), field.type(), DependencyType.USES_COMPONENT.name(),
                        "%s declares injected field %s%s.%s in (%s:0)".formatted(simpleName(owner.name()),
                                injectionAnnotations(field.annotations()), simpleName(owner.name()), field.name(),
                                owner.sourceFile()),
                        allowFieldInjection || facts.isConfiguration(owner.name()) ? ""
                                : owner.name() + "." + field.name())));
    }

    private DependencyType typeOf(String target) {
        return pool.application(target).filter(it -> it.annotations().stream()
                .anyMatch(annotation -> annotation.type().equals(Names.JPA_ENTITY))).isPresent() ? DependencyType.ENTITY
                        : DependencyType.DEFAULT;
    }

    private static Stream<String> classParameters(ScannedMethod method) {
        return method.parameterTypes().stream().filter(DependencyAnalysis::isClass);
    }

    /**
     * @return whether the name is a class Spring Modulith considers: no primitive, array or JDK type
     */
    private static boolean isClass(String name) {
        return name.contains(".") && !name.startsWith("[") && !TypeNames.isCoreType(name);
    }

    /**
     * Describes a member the way Spring Modulith does, for example
     * {@code Inventory declares constructor @Inject Inventory(Orders) in (Inventory.java:0)}.
     */
    private static String describe(ScannedMethod member, String declaringElement, ScannedClass owner, String kind) {
        String simple = simpleName(declaringElement);
        String unit = member.isConstructor() ? simple : simple + "." + member.name();
        String parameters = member.parameterTypes().stream().map(DependencyAnalysis::simpleName)
                .collect(Collectors.joining(", ", "(", ")"));
        return "%s declares %s %s%s%s in (%s:0)".formatted(simple, kind, injectionAnnotations(member.annotations()), unit,
                parameters, owner.sourceFile());
    }

    private static String injectionAnnotations(List<AnnotationData> annotations) {
        return annotations.stream().map(AnnotationData::type).filter(Names.INJECT::contains)
                .map(it -> "@" + simpleName(it) + " ")
                .collect(Collectors.joining());
    }

    /**
     * @return the simple name of a binary class name, {@code String[]} for arrays
     */
    static String simpleName(String name) {
        String readable = name.startsWith("[") ? Type.getType(name.replace('.', '/')).getClassName() : name;
        return readable.substring(Math.max(readable.lastIndexOf('.'), readable.lastIndexOf('$')) + 1);
    }

    private Stream<ScannedClass> superclasses(ScannedClass type) {
        return Stream.iterate(Optional.of(type), Optional::isPresent, it -> pool.application(it.get().superName()))
                .map(Optional::get);
    }
}
