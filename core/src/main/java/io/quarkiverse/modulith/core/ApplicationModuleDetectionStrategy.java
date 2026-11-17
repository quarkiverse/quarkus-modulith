package io.quarkiverse.modulith.core;

import java.util.stream.Stream;

/**
 * Decides which packages are application modules, like Spring Modulith's {@code ApplicationModuleDetectionStrategy}.
 * Select a strategy with {@code quarkus.modulith.detection-strategy}: {@code direct-sub-packages} (the default),
 * {@code explicitly-annotated}, or the fully qualified name of an implementation with a public no-argument
 * constructor.
 */
@FunctionalInterface
public interface ApplicationModuleDetectionStrategy {

    /**
     * @param rootPackage a root package of the application
     * @return the base packages of the modules within the root package
     */
    Stream<JavaPackage> getModuleBasePackages(JavaPackage rootPackage);

    /**
     * Defines the named interfaces of a module. The default discovers them from {@code @NamedInterface}
     * declarations.
     *
     * @param basePackage the module's base package, without nested modules
     * @param information what the module declares about itself
     * @return the named interfaces, the unnamed one included
     */
    default NamedInterfaces detectNamedInterfaces(JavaPackage basePackage, ApplicationModuleInformation information) {
        return NamedInterfaces.of(basePackage, information);
    }

    /**
     * @return a strategy that turns every direct sub-package of the root package into a module
     */
    static ApplicationModuleDetectionStrategy directSubPackage() {
        return pkg -> pkg.getDirectSubPackages().stream();
    }

    /**
     * @return a strategy that only turns packages annotated with {@code @ApplicationModule} or jMolecules'
     *         {@code @Module} into modules
     */
    static ApplicationModuleDetectionStrategy explicitlyAnnotated() {
        return pkg -> pkg.getSubPackagesAnnotatedWith(AnnotationModuleInformation.ALL_ANNOTATIONS);
    }
}
