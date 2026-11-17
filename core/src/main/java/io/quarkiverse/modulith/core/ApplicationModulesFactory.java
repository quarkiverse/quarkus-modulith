package io.quarkiverse.modulith.core;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Creates the {@link ApplicationModules} for an application type, like Spring Modulith's
 * {@code ApplicationModulesFactory}. A project replaces how the model of its module tests is built by registering an
 * implementation in {@code META-INF/services/io.quarkiverse.modulith.core.ApplicationModulesFactory}, usually in the
 * test resources: as Spring Modulith's {@code ModuleTestExecution} does, the build of a module test
 * ({@code @ApplicationModuleTest}, or {@code quarkus.modulith.test.module}) gives the {@code @Modulith} type to the
 * {@link #registered(ClassLoader) registered} factory and uses the modules it returns. Other builds always analyze the
 * application themselves.
 */
@FunctionalInterface
public interface ApplicationModulesFactory {

    /**
     * @return the modules of the application whose base package is the package of the given type
     */
    ApplicationModules of(Class<?> applicationClass);

    /**
     * @return the factory that calls {@link ApplicationModules#of(Class)}
     */
    static ApplicationModulesFactory defaultFactory() {
        return ApplicationModules::of;
    }

    /**
     * @return the first factory registered with the context class loader, or the {@link #defaultFactory()}
     */
    static ApplicationModulesFactory lookup() {
        return registered(Thread.currentThread().getContextClassLoader())
                .orElseGet(ApplicationModulesFactory::defaultFactory);
    }

    /**
     * @return the first factory registered with the given class loader, empty when there is none
     */
    static Optional<ApplicationModulesFactory> registered(ClassLoader classLoader) {
        return ServiceLoader.load(ApplicationModulesFactory.class, classLoader).findFirst();
    }
}
