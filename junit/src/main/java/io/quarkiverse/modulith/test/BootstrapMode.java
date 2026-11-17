package io.quarkiverse.modulith.test;

/**
 * Which modules a module test starts besides the module under test. Shared modules declared with
 * {@code @Modulith(sharedModules = ...)} are always started.
 */
public enum BootstrapMode {

    /**
     * Only the module under test.
     */
    STANDALONE("standalone"),

    /**
     * The module under test and the modules it depends on directly.
     */
    DIRECT_DEPENDENCIES("direct-dependencies"),

    /**
     * The module under test and all the modules it depends on, transitively.
     */
    ALL_DEPENDENCIES("all-dependencies");

    private final String configValue;

    BootstrapMode(String configValue) {
        this.configValue = configValue;
    }

    String configValue() {
        return configValue;
    }
}
