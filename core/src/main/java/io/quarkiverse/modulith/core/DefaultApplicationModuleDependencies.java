package io.quarkiverse.modulith.core;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A list of module dependencies.
 */
final class DefaultApplicationModuleDependencies implements ApplicationModuleDependencies {

    static final ApplicationModuleDependencies NONE = new DefaultApplicationModuleDependencies(List.of());

    private final List<ApplicationModuleDependency> dependencies;
    private final Set<ApplicationModule> modules;

    DefaultApplicationModuleDependencies(List<ApplicationModuleDependency> dependencies) {
        this.dependencies = dependencies;
        this.modules = dependencies.stream().map(ApplicationModuleDependency::getTargetModule)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Stream<ApplicationModuleDependency> stream() {
        return dependencies.stream();
    }

    @Override
    public ApplicationModuleDependencies withType(DependencyType type) {
        return new DefaultApplicationModuleDependencies(
                dependencies.stream().filter(it -> it.getDependencyType() == type).toList());
    }

    @Override
    public Set<ApplicationModule> uniqueModules() {
        return modules;
    }

    @Override
    public boolean containsModuleNamed(String identifier) {
        return modules.stream().anyMatch(it -> it.getName().equals(identifier));
    }

    @Override
    public boolean isEmpty() {
        return dependencies.isEmpty();
    }

    @Override
    public boolean contains(ApplicationModule module) {
        return modules.contains(module);
    }

    @Override
    public boolean contains(String type) {
        return getModuleByType(type).isPresent();
    }

    @Override
    public Stream<ApplicationModuleDependency> uniqueStream(Function<ApplicationModuleDependency, Object> extractor) {
        Set<Object> seen = new HashSet<>();
        return dependencies.stream().filter(it -> seen.add(extractor.apply(it)));
    }

    @Override
    public Optional<ApplicationModule> getModuleByType(String type) {
        return modules.stream().filter(it -> it.contains(type)).findFirst();
    }

    @Override
    public String toString() {
        return dependencies.toString();
    }
}
