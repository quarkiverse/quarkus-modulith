package io.quarkiverse.modulith.core;

import java.util.List;
import java.util.Optional;

/**
 * What a module declares about itself through {@code @ApplicationModule}, Spring Modulith's annotation of the same
 * name or jMolecules' {@code @Module}.
 */
public interface ApplicationModuleInformation {

    /**
     * Returned by {@link #getDeclaredDependencies()} when the module does not restrict its dependencies.
     */
    String OPEN_TOKEN = io.quarkiverse.modulith.ApplicationModule.OPEN_TOKEN;

    /**
     * @return the information declared on the given package, the package itself without its sub-packages
     */
    static ApplicationModuleInformation of(JavaPackage javaPackage) {
        return AnnotationModuleInformation.of(javaPackage);
    }

    /**
     * @return the declared display name, if any
     */
    default Optional<String> getDisplayName() {
        return Optional.empty();
    }

    /**
     * @return the declared allowed dependencies, or a list holding only {@link #OPEN_TOKEN} when there is no
     *         restriction
     */
    List<String> getDeclaredDependencies();

    /**
     * @return whether the module is declared open
     */
    boolean isOpen();
}
