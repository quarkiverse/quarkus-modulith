package io.quarkiverse.modulith.core;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * A package that forms an application module, with the module's identifier and how its named interfaces are found.
 * Like Spring Modulith's {@code ApplicationModuleSource}.
 */
public final class ApplicationModuleSource {

    private final JavaPackage moduleBasePackage;
    private final ApplicationModuleIdentifier identifier;
    private final BiFunction<JavaPackage, ApplicationModuleInformation, NamedInterfaces> namedInterfacesFactory;

    private ApplicationModuleSource(JavaPackage moduleBasePackage, ApplicationModuleIdentifier identifier,
            BiFunction<JavaPackage, ApplicationModuleInformation, NamedInterfaces> namedInterfacesFactory) {
        this.moduleBasePackage = moduleBasePackage;
        this.identifier = identifier;
        this.namedInterfacesFactory = namedInterfacesFactory;
    }

    /**
     * Detects the modules of a root package with the given strategy, plus the sub-packages of each module annotated
     * with {@code @ApplicationModule} or jMolecules' {@code @Module}, which become nested modules.
     *
     * @param rootPackage the root package
     * @param strategy the detection strategy
     * @param fullyQualifiedModuleNames whether undeclared identifiers are full package names rather than the name
     *        relative to the root package
     */
    public static Stream<ApplicationModuleSource> from(JavaPackage rootPackage, ApplicationModuleDetectionStrategy strategy,
            boolean fullyQualifiedModuleNames) {
        return strategy.getModuleBasePackages(rootPackage)
                .flatMap(it -> Stream.concat(Stream.of(it),
                        it.getSubPackagesAnnotatedWith(AnnotationModuleInformation.ALL_ANNOTATIONS)))
                .map(it -> {
                    ApplicationModuleIdentifier id = AnnotationModuleInformation.of(it).getIdentifier()
                            .map(ApplicationModuleIdentifier::of)
                            .orElseGet(() -> ApplicationModuleIdentifier.of(
                                    fullyQualifiedModuleNames ? it.getName() : rootPackage.getTrailingName(it)));
                    return new ApplicationModuleSource(it, id, strategy::detectNamedInterfaces);
                });
    }

    /**
     * @return a module with the given package and identifier, whose named interfaces are discovered from
     *         annotations
     */
    public static ApplicationModuleSource from(JavaPackage pkg, String identifier) {
        return new ApplicationModuleSource(pkg, ApplicationModuleIdentifier.of(identifier), NamedInterfaces::of);
    }

    public JavaPackage getModuleBasePackage() {
        return moduleBasePackage;
    }

    public ApplicationModuleIdentifier getIdentifier() {
        return identifier;
    }

    /**
     * @return the named interfaces of the module with the given base package
     */
    public NamedInterfaces getNamedInterfaces(ApplicationModuleInformation information, JavaPackage basePackage) {
        return namedInterfacesFactory.apply(basePackage, information);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ApplicationModuleSource that && identifier.equals(that.identifier)
                && moduleBasePackage.getName().equals(that.moduleBasePackage.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, moduleBasePackage.getName());
    }

    @Override
    public String toString() {
        return "ApplicationModuleSource(" + identifier + ", " + moduleBasePackage.getName() + ")";
    }
}
