package io.quarkiverse.modulith.docs;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.DependencyDepth;
import io.quarkiverse.modulith.core.DependencyType;
import io.quarkiverse.modulith.docs.metadata.DocumentationSource;

/**
 * The input of the {@link Documenter}: the application modules plus the {@link ModuleDetails} of each module, the
 * shared modules and the Javadoc of the application.
 * <p>
 * The build adds what only it knows from the Jandex index, such as the "Scheduled jobs" and "Messaging endpoints"
 * groups, to the model {@link #of(ApplicationModules)} derives from the core module model.
 */
public final class DocumentationModel {

    private static final String DEFAULT_SYSTEM_NAME = "Modules";

    private final ApplicationModules modules;
    private final Map<String, ModuleDetails> details;
    private final List<String> sharedModules;
    private final DocumentationSource documentation;

    /**
     * @param modules the application modules
     * @param details the details per module, modules without details get {@link ModuleDetails#of(String)}
     * @param sharedModules the identifiers of the shared modules
     * @param documentation the Javadoc of the application
     */
    public DocumentationModel(ApplicationModules modules, List<ModuleDetails> details, List<String> sharedModules,
            DocumentationSource documentation) {
        this.modules = Objects.requireNonNull(modules, "modules");
        this.details = details.stream()
                .collect(Collectors.toUnmodifiableMap(ModuleDetails::identifier, Function.identity()));
        this.sharedModules = List.copyOf(sharedModules);
        this.documentation = Objects.requireNonNull(documentation, "documentation");
    }

    /**
     * Derives the details from the core module model, as Spring Modulith's documenter reads its
     * {@code ApplicationModules}, with the Javadoc found on the class path. The configuration properties are read from
     * the module types loaded with the context class loader.
     */
    public static DocumentationModel of(ApplicationModules modules) {
        DocumentationSource documentation = DocumentationSource.fromClassPath();
        ConfigProperties properties = new ConfigProperties(documentation);
        List<ModuleDetails> details = modules.stream().map(module -> CoreModels.details(modules, module, properties))
                .toList();
        List<String> shared = modules.getSharedModules().stream().map(ApplicationModule::getName).toList();
        return new DocumentationModel(modules, details, shared, documentation);
    }

    public ApplicationModules modules() {
        return modules;
    }

    public DocumentationSource documentation() {
        return documentation;
    }

    /**
     * @return the system name, {@code Modules} when the application declares none
     */
    public String systemName() {
        String name = modules.getSystemName();
        return Objects.isNull(name) || name.isBlank() ? DEFAULT_SYSTEM_NAME : name;
    }

    public ModuleDetails details(ApplicationModule module) {
        return Optional.ofNullable(details.get(module.getName())).orElseGet(() -> ModuleDetails.of(module.getName()));
    }

    /**
     * @return the module containing the type with the given name
     */
    public Optional<ApplicationModule> moduleByType(String type) {
        return modules.getModuleByType(type);
    }

    /**
     * @param name a binary type name, or a simple name, which is looked up in every module in module order
     * @return the type with the given name, if it belongs to a module
     */
    public Optional<ModuleType> type(String name) {
        if (TypeNames.packageOf(name).isEmpty()) {
            return modules.stream()
                    .flatMap(module -> details(module).types().stream())
                    .filter(it -> TypeNames.simpleName(it.name()).equals(name))
                    .findFirst();
        }
        return moduleByType(name).flatMap(module -> details(module).type(name));
    }

    /**
     * @return whether other modules may use the type: it is not part of a module, it is a known exposed type, or it
     *         lives in its module's base package
     */
    public boolean isExposed(String type) {
        return moduleByType(type)
                .map(module -> details(module).type(type).map(ModuleType::exposed)
                        .orElseGet(() -> TypeNames.packageOf(type).equals(module.getBasePackage())))
                .orElse(true);
    }

    public List<ApplicationModule> sharedModules() {
        return sharedModules.stream().map(modules::getModuleByName).flatMap(Optional::stream).toList();
    }

    public Optional<ApplicationModule> parentOf(ApplicationModule module) {
        return details(module).parent().flatMap(modules::getModuleByName);
    }

    public boolean hasParent(ApplicationModule module) {
        return parentOf(module).isPresent();
    }

    public List<ApplicationModule> nestedModules(ApplicationModule module) {
        return modules.stream()
                .filter(candidate -> details(candidate).parent().filter(module.getName()::equals).isPresent())
                .toList();
    }

    /**
     * @return the modules the given module references with one of the given types, all types when none is given, in
     *         the order of the dependencies
     */
    public List<ApplicationModule> directDependencies(ApplicationModule module, DependencyType... types) {
        Set<DependencyType> wanted = types.length == 0 ? Set.of(DependencyType.values()) : Set.copyOf(Arrays.asList(types));
        return details(module).dependencies().stream()
                .filter(dependency -> wanted.contains(dependency.type()))
                .map(ModuleDependency::targetModule)
                .distinct()
                .map(modules::getModuleByName)
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * @return the modules whose beans the given module injects, directly or, with {@link DependencyDepth#ALL},
     *         transitively
     */
    public List<ApplicationModule> bootstrapDependencies(ApplicationModule module, DependencyDepth depth) {
        if (depth == DependencyDepth.NONE) {
            return List.of();
        }
        List<ApplicationModule> direct = directDependencies(module, DependencyType.USES_COMPONENT);
        if (depth == DependencyDepth.IMMEDIATE) {
            return direct;
        }
        Set<ApplicationModule> result = new LinkedHashSet<>();
        for (ApplicationModule dependency : direct) {
            collect(dependency, result);
        }
        return List.copyOf(result);
    }

    private void collect(ApplicationModule module, Set<ApplicationModule> result) {
        if (result.add(module)) {
            directDependencies(module, DependencyType.USES_COMPONENT).forEach(it -> collect(it, result));
        }
    }

    /**
     * @return the dependency types per target module, without {@link DependencyType#DEFAULT} when another type
     *         exists for the same target, in the order of the dependencies
     */
    Map<ApplicationModule, Set<DependencyType>> dependencyTypesByModule(ApplicationModule module) {
        Map<ApplicationModule, Set<DependencyType>> result = new LinkedHashMap<>();
        for (ModuleDependency dependency : details(module).dependencies()) {
            modules.getModuleByName(dependency.targetModule()).ifPresent(target -> result
                    .computeIfAbsent(target, it -> new TreeSet<>()).add(dependency.type()));
        }
        result.values().forEach(types -> {
            if (types.size() > 1) {
                types.remove(DependencyType.DEFAULT);
            }
        });
        return result;
    }
}
