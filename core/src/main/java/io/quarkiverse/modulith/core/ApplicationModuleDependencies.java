package io.quarkiverse.modulith.core;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The dependencies of a module, like Spring Modulith's {@code ApplicationModuleDependencies}.
 */
public interface ApplicationModuleDependencies {

    Stream<ApplicationModuleDependency> stream();

    /**
     * @return the dependencies of the given type only
     */
    ApplicationModuleDependencies withType(DependencyType type);

    /**
     * @return the modules depended on, each once, in the order they are first depended on
     */
    Set<ApplicationModule> uniqueModules();

    boolean containsModuleNamed(String identifier);

    boolean isEmpty();

    /**
     * @return whether one of the dependencies targets the given module
     */
    boolean contains(ApplicationModule module);

    /**
     * @return whether one of the modules depended on contains the type with the given simple or fully qualified name
     */
    boolean contains(String type);

    /**
     * @return the dependencies, the first one for each value the extractor returns
     */
    Stream<ApplicationModuleDependency> uniqueStream(Function<ApplicationModuleDependency, Object> extractor);

    /**
     * @return the module depended on that contains the type with the given simple or fully qualified name
     */
    Optional<ApplicationModule> getModuleByType(String type);
}
