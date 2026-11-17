package io.quarkiverse.modulith.core;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Contributes root packages and modules from a library, like Spring Modulith's {@code ApplicationModuleSourceFactory}.
 * Implementations are registered in {@code META-INF/services/io.quarkiverse.modulith.core.ApplicationModuleSourceFactory}
 * and need a public no-argument constructor.
 */
public interface ApplicationModuleSourceFactory {

    /**
     * @return further root packages to detect modules in, with {@link #getApplicationModuleDetectionStrategy()}
     */
    default List<String> getRootPackages() {
        return List.of();
    }

    /**
     * @return the base packages of further modules, each a module of its own
     */
    default List<String> getModuleBasePackages() {
        return List.of();
    }

    /**
     * @return the strategy for {@link #getRootPackages()}, {@code null} to use the configured one. Nullable instead of an
     *         {@code Optional} to keep the signature of Spring Modulith's SPI, so that factories port unchanged.
     */
    default ApplicationModuleDetectionStrategy getApplicationModuleDetectionStrategy() {
        return null;
    }

    /**
     * @return the modules of one of the contributed root packages
     */
    default Stream<ApplicationModuleSource> getApplicationModuleSources(JavaPackage rootPackage,
            ApplicationModuleDetectionStrategy strategy, boolean useFullyQualifiedModuleNames) {
        return ApplicationModuleSource.from(rootPackage, strategy, useFullyQualifiedModuleNames);
    }

    /**
     * @param packages resolves a package name
     * @return the modules of {@link #getModuleBasePackages()}
     */
    default Stream<ApplicationModuleSource> getApplicationModuleSources(Function<String, JavaPackage> packages,
            boolean useFullyQualifiedModuleNames) {
        return getModuleBasePackages().stream()
                .map(packages)
                .map(it -> ApplicationModuleSource.from(it, useFullyQualifiedModuleNames ? it.getName() : it.getLocalName()));
    }
}
