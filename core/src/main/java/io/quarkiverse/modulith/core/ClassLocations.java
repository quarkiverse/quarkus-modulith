package io.quarkiverse.modulith.core;

/**
 * Which class files {@link ApplicationModules#of(Class, ClassLocations)} reads.
 */
public enum ClassLocations {

    /**
     * Production classes only: test output directories of Maven, Gradle and IDEs are left out. The default.
     */
    PRODUCTION,

    /**
     * Classes of test output directories only, for test fixtures that form an application of their own.
     */
    TESTS,

    /**
     * Production and test classes.
     */
    ALL
}
