package io.quarkiverse.modulith.deployment.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import io.quarkiverse.modulith.core.ApplicationModule;
import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.JavaType;
import io.quarkiverse.modulith.core.NamedInterface;

/**
 * An application module as other build steps see it: an immutable view of a core {@link ApplicationModule}.
 */
public final class ModuleDescriptor {

    private final ApplicationModule module;
    private final List<ModuleNamedInterface> namedInterfaces;
    private final Set<String> classes;
    private final Map<String, Set<ModuleReference>> referencesByModule;
    private final Set<String> publishedEvents;
    private final Set<String> listenedEvents;

    ModuleDescriptor(ApplicationModule module, ApplicationModules modules) {
        this.module = module;
        this.namedInterfaces = module.getNamedInterfaceDetails().stream()
                .filter(NamedInterface::isNamed)
                .map(it -> new ModuleNamedInterface(it.getName(), Collections.unmodifiableSet(new TreeSet<>(it.getPackages())),
                        Collections.unmodifiableSet(new TreeSet<>(it.asJavaTypes().map(JavaType::getName).toList()))))
                .toList();
        this.classes = Collections
                .unmodifiableSet(new TreeSet<>(module.getJavaPackage().stream().map(JavaType::getName).toList()));
        Map<String, Set<ModuleReference>> references = new TreeMap<>();
        module.getDirectDependencies(modules).stream().forEach(it -> references
                .computeIfAbsent(it.getTargetModule().getName(), key -> new TreeSet<>())
                .add(new ModuleReference(it.getSourceType(), it.getTargetType())));
        Map<String, Set<ModuleReference>> frozen = new LinkedHashMap<>();
        references.forEach((key, value) -> frozen.put(key, Collections.unmodifiableSet(value)));
        this.referencesByModule = Collections.unmodifiableMap(frozen);
        this.publishedEvents = Collections.unmodifiableSet(new TreeSet<>(module.getPublishedEvents()));
        this.listenedEvents = Collections.unmodifiableSet(new TreeSet<>(module.getListenedEvents()));
    }

    /**
     * @return the core module this view shows
     */
    public ApplicationModule applicationModule() {
        return module;
    }

    public String name() {
        return module.getName();
    }

    public String displayName() {
        return module.getDisplayName();
    }

    public String basePackage() {
        return module.getBasePackage();
    }

    public boolean open() {
        return module.isOpen();
    }

    /**
     * @return the declared allowed dependencies, empty when the module does not restrict them
     */
    public Optional<List<String>> allowedDependencies() {
        return module.getAllowedDependencies();
    }

    /**
     * @return the named interfaces, without the unnamed one
     */
    public List<ModuleNamedInterface> namedInterfaces() {
        return namedInterfaces;
    }

    public Set<String> classes() {
        return classes;
    }

    /**
     * @return the references from this module to the types of other modules, grouped by target module
     */
    public Map<String, Set<ModuleReference>> referencesByModule() {
        return referencesByModule;
    }

    public Set<String> dependencies() {
        return referencesByModule.keySet();
    }

    public Set<String> publishedEvents() {
        return publishedEvents;
    }

    public Set<String> listenedEvents() {
        return listenedEvents;
    }

    /**
     * @return whether other modules may reference the type: it belongs to an interface of the module, or the module is
     *         open
     */
    public boolean exposes(String className) {
        return open() || module.getNamedInterfaceDetails().getNamedInterfacesContaining(className).findAny().isPresent();
    }

    public boolean isInBasePackage(String className) {
        int dot = className.lastIndexOf('.');
        return dot > 0 && className.substring(0, dot).equals(basePackage());
    }

    public boolean contains(String className) {
        return classes.contains(className);
    }

    /**
     * @return the names of the types other modules may use
     */
    public Set<String> exposedTypes() {
        return classes.stream().filter(it -> !it.endsWith(".package-info") && exposes(it))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String toString() {
        return name() + " (" + basePackage() + ")";
    }
}
