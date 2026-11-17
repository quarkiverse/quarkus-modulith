package io.quarkiverse.modulith.core;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkiverse.modulith.core.ModelData.ModuleData;
import io.quarkiverse.modulith.core.ModelData.NamedInterfaceData;
import io.quarkiverse.modulith.core.data.DependencyData;

/**
 * An application module, like Spring Modulith's {@code ApplicationModule}.
 * <p>
 * Where Spring Modulith returns ArchUnit types this API returns {@link JavaType}. Where the frozen Quarkus Modulith API
 * predates a Spring method with a different return type, the Spring method is available under another name:
 * {@link #getJavaPackage()} for {@code getBasePackage()}, {@link #getNamedInterfaceDetails()} for
 * {@code getNamedInterfaces()} and {@link #getPublishedEventTypes()} for {@code getPublishedEvents()}.
 */
public final class ApplicationModule implements Comparable<ApplicationModule> {

    private final ModuleData data;
    private final Map<String, JavaType> index;
    private final boolean rootModule;
    private final List<JavaType> types;
    private final Map<String, JavaType> typesByName = new HashMap<>();
    private final Map<String, JavaType> typesBySimpleName = new HashMap<>();
    private final JavaPackage javaPackage;
    private final NamedInterfaces namedInterfaces;
    private volatile ResolvedDependencies resolvedDependencies;

    ApplicationModule(ModuleData data, Map<String, JavaType> index, boolean rootModule) {
        this.data = data;
        this.index = index;
        this.rootModule = rootModule;
        this.types = typesOf(data.types());
        types.forEach(it -> {
            typesByName.put(it.getName(), it);
            typesBySimpleName.putIfAbsent(it.getSimpleName(), it);
        });
        this.javaPackage = JavaPackage.of(types, data.basePackage());
        this.namedInterfaces = NamedInterfaces.of(data.namedInterfaces().stream()
                .map(it -> NamedInterface.of(it.name(), typesOf(it.types()), it.packages()))
                .toList());
    }

    private List<JavaType> typesOf(List<String> names) {
        return names.stream().map(index::get).filter(Objects::nonNull).toList();
    }

    /**
     * @return the module identifier, by default the name of its base package relative to the root package
     */
    public String getName() {
        return data.name();
    }

    /**
     * @return the identifier as a value type, same as {@link #getName()}
     */
    public ApplicationModuleIdentifier getIdentifier() {
        return ApplicationModuleIdentifier.of(data.name());
    }

    /**
     * @return the declared display name, or the capitalized last segment of the base package
     */
    public String getDisplayName() {
        return data.displayName();
    }

    /**
     * @return the name of the base package
     */
    public String getBasePackage() {
        return data.basePackage();
    }

    /**
     * @return the base package with the module's types, without those of nested modules
     */
    public JavaPackage getJavaPackage() {
        return javaPackage;
    }

    /**
     * @return whether the module is open, that is exposes all its types and is left out of cycle detection
     */
    public boolean isOpen() {
        return data.open();
    }

    /**
     * @return whether this is the artificial module holding the types of a root package itself
     */
    public boolean isRootModule() {
        return rootModule;
    }

    /**
     * @return the declared allowed dependencies, or empty if the module does not restrict its dependencies
     */
    public Optional<List<String>> getAllowedDependencies() {
        return data.restricted() ? Optional.of(data.allowedDependencies()) : Optional.empty();
    }

    /**
     * @return the declared allowed dependencies as parsed grants, plus the shared modules of the application
     * @throws IllegalArgumentException if a declaration names a module or named interface that does not exist
     */
    public AllowedDependencies getAllowedDependencies(ApplicationModules modules) {
        return getAllowedDependencies(modules, it -> Optional.of(AllowedDependency.of(it, this, modules)));
    }

    /**
     * @param parser parses a declaration, empty to leave an invalid one out
     */
    AllowedDependencies getAllowedDependencies(ApplicationModules modules,
            Function<String, Optional<AllowedDependency>> parser) {
        if (!data.restricted()) {
            return AllowedDependencies.open();
        }
        Stream<AllowedDependency> declared = data.allowedDependencies().stream()
                .flatMap(it -> parser.apply(it).stream());
        Stream<AllowedDependency> shared = modules.getSharedModules().stream()
                .filter(it -> !it.equals(this))
                .map(AllowedDependency::to);
        return AllowedDependencies.closed(Stream.concat(declared, shared).distinct().toList());
    }

    /**
     * @return the names of the module's named interfaces, without the unnamed one
     */
    public Set<String> getNamedInterfaces() {
        return data.namedInterfaces().stream().map(NamedInterfaceData::name)
                .filter(it -> !it.equals(NamedInterface.UNNAMED_NAME))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * @return all named interfaces, the unnamed one included
     */
    public NamedInterfaces getNamedInterfaceDetails() {
        return namedInterfaces;
    }

    /**
     * @return the identifiers of the modules this module depends on directly
     */
    public List<String> getDependencies() {
        return data.dependencies();
    }

    /**
     * @return whether this module depends on the given module
     */
    public boolean dependsOn(String moduleName) {
        return data.dependencies().contains(moduleName);
    }

    /**
     * @return fully qualified names of the event types the module publishes: types fired through an injected
     *         {@code jakarta.enterprise.event.Event}, and jMolecules domain events declared in the module
     */
    public List<String> getPublishedEvents() {
        return data.publishedEvents();
    }

    /**
     * @return the published event types with the places creating them
     */
    public List<EventType> getPublishedEventTypes() {
        return data.events().stream().map(it -> new EventType(index.get(it.type()), it)).toList();
    }

    /**
     * @return fully qualified names of the event types the module listens to
     */
    public List<String> getListenedEvents() {
        return data.listenedEvents();
    }

    /**
     * @return the types of other modules this module listens to, like Spring Modulith's {@code getEventsListenedTo}
     */
    public List<JavaType> getEventsListenedTo(ApplicationModules modules) {
        return qualifiedDependencies(modules)
                .filter(it -> it.getDependencyType() == DependencyType.EVENT_LISTENER)
                .map(ApplicationModuleDependency::getTargetJavaType)
                .distinct()
                .toList();
    }

    /**
     * @return the value types of the module
     */
    public List<JavaType> getValueTypes() {
        return typesOf(data.valueTypes());
    }

    /**
     * @return the aggregate roots of the module
     */
    public List<JavaType> getAggregateRoots() {
        return typesOf(data.aggregateRoots());
    }

    /**
     * @return the beans of the module
     */
    public List<ModuleBean> getBeans() {
        return typesOf(data.beans()).stream().map(it -> new ModuleBean(it, this)).toList();
    }

    /**
     * @return the beans assignable to the given type
     */
    public List<ModuleBean> getBeans(Class<?> type) {
        return getBeans().stream().filter(it -> it.isAssignableTo(type)).toList();
    }

    /**
     * @deprecated use {@link #getBeans()}
     */
    @Deprecated
    public List<ModuleBean> getSpringBeans() {
        return getBeans();
    }

    /**
     * @deprecated use {@link #getBeans(Class)}
     */
    @Deprecated
    public List<ModuleBean> getSpringBeans(Class<?> type) {
        return getBeans(type);
    }

    /**
     * @return the methods the framework calls into the module
     */
    public List<EntryPoint> getEntryPoints() {
        return data.entryPoints().stream().map(EntryPoint::of).toList();
    }

    /**
     * @throws IllegalArgumentException if the type does not belong to the module
     */
    public ArchitecturallyEvidentType getArchitecturallyEvidentType(Class<?> type) {
        return getType(type.getName()).map(ArchitecturallyEvidentType::of)
                .orElseThrow(() -> new IllegalArgumentException("Couldn't find type %s in module %s!".formatted(
                        FormattableType.of(type).getAbbreviatedFullName(this), getIdentifier())));
    }

    /**
     * @return the type with the given simple or fully qualified name, if it belongs to the module
     */
    public Optional<JavaType> getType(String candidate) {
        return Optional.ofNullable(candidate.indexOf('.') < 0 ? typesBySimpleName.get(candidate)
                : typesByName.get(candidate));
    }

    /**
     * @return the types of the module that no interface exposes
     */
    public List<JavaType> getInternalTypes() {
        return types.stream().filter(it -> !isExposed(it)).toList();
    }

    /**
     * @return whether one of the module's interfaces, the unnamed one included, contains the type
     */
    public boolean isExposed(JavaType type) {
        return namedInterfaces.getNamedInterfacesContaining(type).findAny().isPresent();
    }

    public boolean isExposed(Class<?> type) {
        return namedInterfaces.getNamedInterfacesContaining(type.getName()).findAny().isPresent();
    }

    /**
     * @return whether the module contains the type with the given simple or fully qualified name
     */
    public boolean contains(String candidate) {
        if (types.isEmpty()) {
            return candidate.startsWith(data.basePackage() + ".");
        }
        PackageName candidatePackage = PackageName.ofType(candidate);
        return (PackageName.isDefault(candidatePackage) || javaPackage.getPackageName().contains(candidatePackage))
                && getType(candidate).isPresent();
    }

    public boolean contains(Class<?> type) {
        return contains(type.getName());
    }

    public boolean contains(JavaType type) {
        return typesByName.containsKey(type.getName());
    }

    /**
     * @return whether the type would belong to the module, also when it is not part of the analysed types, for example
     *         a test type in the module's package
     */
    public boolean couldContain(Class<?> type) {
        return couldContain(type.getName());
    }

    boolean couldContain(String typeName) {
        return contains(typeName) || (!isInExcludedPackage(typeName) && javaPackage.couldContain(typeName));
    }

    /**
     * @return whether the type lives in the base package of a nested module
     */
    private boolean isInExcludedPackage(String typeName) {
        PackageName typePackage = PackageName.ofType(typeName);
        return data.excludedPackages().stream().anyMatch(it -> PackageName.of(it).contains(typePackage));
    }

    /**
     * @return whether the given package is the module's base package or one of its sub-packages
     */
    boolean containsPackage(String packageName) {
        return PackageName.of(data.basePackage()).contains(PackageName.of(packageName));
    }

    boolean hasBasePackage(String candidate) {
        return data.basePackage().equals(candidate);
    }

    /**
     * @return the modules this module refers to directly, with the kind of each reference
     */
    public ApplicationModuleDependencies getDirectDependencies(ApplicationModules modules) {
        return getDependencies(modules, DependencyDepth.IMMEDIATE);
    }

    /**
     * @return the direct dependencies of the given types, all types when none is given
     */
    public ApplicationModuleDependencies getDirectDependencies(ApplicationModules modules, DependencyType... types) {
        return getDependencies(modules, DependencyDepth.IMMEDIATE, types);
    }

    /**
     * @return the direct dependencies of the given types, all types when none is given
     */
    public ApplicationModuleDependencies getDependencies(ApplicationModules modules, DependencyType... types) {
        return getDependencies(modules, DependencyDepth.IMMEDIATE, types);
    }

    /**
     * @return the direct and transitive dependencies
     */
    public ApplicationModuleDependencies getAllDependencies(ApplicationModules modules) {
        return getDependencies(modules, DependencyDepth.ALL);
    }

    /**
     * @return the direct and transitive dependencies of the given types, all types when none is given
     */
    public ApplicationModuleDependencies getAllDependencies(ApplicationModules modules, DependencyType... types) {
        return getDependencies(modules, DependencyDepth.ALL, types);
    }

    /**
     * @return the dependencies of the given types up to the given depth, all types when none is given
     */
    public ApplicationModuleDependencies getDependencies(ApplicationModules modules, DependencyDepth depth,
            DependencyType... types) {
        if (depth == DependencyDepth.NONE) {
            return DefaultApplicationModuleDependencies.NONE;
        }
        return new DefaultApplicationModuleDependencies(dependencies(modules, new DependencyTraversal(depth, types)).toList());
    }

    private Stream<ApplicationModuleDependency> dependencies(ApplicationModules modules, DependencyTraversal traversal) {
        return qualifiedDependencies(modules)
                .filter(it -> traversal.include(it.getDependencyType()))
                .flatMap(it -> resolveRecursively(modules, it, traversal));
    }

    private static Stream<ApplicationModuleDependency> resolveRecursively(ApplicationModules modules,
            ApplicationModuleDependency dependency, DependencyTraversal traversal) {
        if (!traversal.register(dependency.getTargetModule()) || !traversal.isTransitive()) {
            return Stream.of(dependency);
        }
        return Stream.concat(Stream.of(dependency), dependency.getTargetModule().dependencies(modules, traversal));
    }

    /**
     * @return the references from this module to the types of other modules, each once, resolved once per model
     */
    Stream<DefaultApplicationModuleDependency> qualifiedDependencies(ApplicationModules modules) {
        ResolvedDependencies resolved = resolvedDependencies;
        if (Objects.isNull(resolved) || resolved.modules() != modules) {
            resolved = new ResolvedDependencies(modules, data.qualifiedDependencies().stream().distinct()
                    .flatMap(it -> modules.getModuleByType(it.target())
                            .map(target -> new DefaultApplicationModuleDependency(it, index.get(it.source()),
                                    index.get(it.target()), target))
                            .stream())
                    .toList());
            resolvedDependencies = resolved;
        }
        return resolved.dependencies().stream();
    }

    List<DependencyData> getQualifiedDependencyData() {
        return data.qualifiedDependencies();
    }

    /**
     * @return the modules a module test of this module starts besides it, for direct dependencies
     */
    public List<ApplicationModule> getBootstrapDependencies(ApplicationModules modules) {
        return getBootstrapDependencies(modules, DependencyDepth.IMMEDIATE);
    }

    /**
     * @return the modules whose beans the beans of this module inject, directly or up to the given depth
     */
    public List<ApplicationModule> getBootstrapDependencies(ApplicationModules modules, DependencyDepth depth) {
        return switch (depth) {
            case NONE -> List.of();
            case IMMEDIATE -> directBootstrapDependencies(modules).toList();
            case ALL -> allBootstrapDependencies(modules, new HashSet<>(Set.of(getName()))).toList();
        };
    }

    private Stream<ApplicationModule> allBootstrapDependencies(ApplicationModules modules, Set<String> seen) {
        return directBootstrapDependencies(modules)
                .filter(it -> seen.add(it.getName()))
                .toList().stream()
                .flatMap(it -> Stream.concat(Stream.of(it), it.allBootstrapDependencies(modules, seen)));
    }

    private Stream<ApplicationModule> directBootstrapDependencies(ApplicationModules modules) {
        return qualifiedDependencies(modules)
                .filter(it -> it.getDependencyType() == DependencyType.USES_COMPONENT)
                .filter(it -> data.beans().contains(it.getSourceType()))
                .map(ApplicationModuleDependency::getTargetModule)
                .distinct();
    }

    /**
     * @return the base packages of this module and of its bootstrap dependencies up to the given depth
     */
    public Stream<JavaPackage> getBootstrapBasePackages(ApplicationModules modules, DependencyDepth depth) {
        return Stream.concat(Stream.of(this), getBootstrapDependencies(modules, depth).stream())
                .map(ApplicationModule::getJavaPackage);
    }

    /**
     * Throws the violations of this module's dependencies, if any.
     */
    public void verifyDependencies(ApplicationModules modules) {
        detectDependencies(modules).throwIfPresent();
    }

    /**
     * @return the violations of this module's dependencies
     */
    public Violations detectDependencies(ApplicationModules modules) {
        return ModuleVerifier.detectDependencies(this, modules);
    }

    /**
     * @return the module whose base package is the closest parent package of this module's base package
     */
    Optional<ApplicationModule> getParentModule(ApplicationModules modules) {
        PackageName name = PackageName.of(data.basePackage());
        return modules.stream()
                .filter(it -> name.isSubPackageOf(PackageName.of(it.getBasePackage())))
                .max(Comparator.comparing(it -> PackageName.of(it.getBasePackage())));
    }

    /**
     * @return the modules nested in this module, transitively
     */
    List<ApplicationModule> getNestedModules(ApplicationModules modules) {
        return getDirectlyNestedModules(modules).stream()
                .flatMap(it -> Stream.concat(Stream.of(it), it.getNestedModules(modules).stream()))
                .toList();
    }

    List<ApplicationModule> getDirectlyNestedModules(ApplicationModules modules) {
        return modules.stream().filter(it -> modules.getParentOf(it).filter(this::equals).isPresent()).toList();
    }

    /**
     * @return whether the type belongs to one of the parent modules of this module
     */
    boolean containsTypeInAnyParent(JavaType type, ApplicationModules modules) {
        return modules.getParentOf(this).filter(it -> it.contains(type) || it.containsTypeInAnyParent(type, modules))
                .isPresent();
    }

    /**
     * @return a description of the module, its interfaces, dependencies and beans
     */
    public String toString(ApplicationModules modules) {
        StringBuilder builder = new StringBuilder("# ").append(getDisplayName());
        if (isOpen()) {
            builder.append(" (open)");
        }
        builder.append('\n');
        Optional.ofNullable(modules).flatMap(it -> it.getParentOf(this))
                .ifPresent(it -> builder.append("> Parent module: ").append(it.getIdentifier()).append('\n'));
        builder.append("> Logical name: ").append(getIdentifier()).append('\n');
        builder.append("> Base package: ").append(getBasePackage()).append('\n');
        builder.append("> Excluded packages: ");
        builder.append(data.excludedPackages().isEmpty() ? "none\n"
                : data.excludedPackages().stream().map(it -> "  - " + it + "\n").collect(Collectors.joining("", "\n", "")));
        if (namedInterfaces.hasExplicitInterfaces()) {
            builder.append("> Named interfaces:\n");
            namedInterfaces.forEach(it -> builder.append("  + ").append(it).append('\n'));
        }
        Optional.ofNullable(modules).ifPresent(it -> {
            ApplicationModuleDependencies direct = getDirectDependencies(it);
            List<String> bootstrap = getBootstrapDependencies(it).stream().map(ApplicationModule::getName).toList();
            builder.append("> Direct module dependencies: ")
                    .append(direct.isEmpty() ? "none"
                            : direct.uniqueModules().stream().sorted()
                                    .map(ApplicationModule::getName)
                                    .map(name -> (bootstrap.contains(name) ? "  + " : "  - ") + name)
                                    .collect(Collectors.joining("\n", "\n", "")))
                    .append('\n');
        });
        List<ModuleBean> beans = getBeans();
        builder.append(beans.isEmpty() ? "> Beans: none\n" : "> Beans:\n");
        beans.forEach(it -> builder.append("  ").append(isExposed(it.getType()) ? "+ " : "o ")
                .append(it.getFullyQualifiedTypeName().replace(getBasePackage(), "…")).append('\n'));
        return builder.toString();
    }

    @Override
    public int compareTo(ApplicationModule other) {
        return PackageName.of(data.basePackage()).compareTo(PackageName.of(other.data.basePackage()));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ApplicationModule that && data.name().equals(that.data.name())
                && data.basePackage().equals(that.data.basePackage()) && rootModule == that.rootModule;
    }

    @Override
    public int hashCode() {
        return Objects.hash(data.name(), data.basePackage(), rootModule);
    }

    @Override
    public String toString() {
        return toString(null);
    }
}
