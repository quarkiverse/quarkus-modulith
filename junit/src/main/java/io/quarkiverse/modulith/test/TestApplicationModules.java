package io.quarkiverse.modulith.test;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.core.ClassLocations;

/**
 * Builds the module model of test code only, without starting Quarkus, for example to verify the structure of test
 * fixtures that form an application of their own. The Quarkus counterpart of Spring Modulith's
 * {@code TestApplicationModules}.
 */
public final class TestApplicationModules {

    private TestApplicationModules() {
    }

    /**
     * @return the modules of the test classes below the package of the given type
     */
    public static ApplicationModules of(Class<?> applicationClass) {
        return ApplicationModules.of(applicationClass, ClassLocations.TESTS);
    }

    /**
     * @return the modules of the test classes below the given package
     */
    public static ApplicationModules of(String basePackage) {
        return ApplicationModules.of(basePackage, ClassLocations.TESTS);
    }
}
