package io.quarkiverse.modulith.core;

/**
 * How far to follow module dependencies, for dependency queries and module tests.
 */
public enum DependencyDepth {

    /**
     * Only the module itself.
     */
    NONE,

    /**
     * The module and the modules it depends on directly.
     */
    IMMEDIATE,

    /**
     * The module and all the modules it depends on, transitively.
     */
    ALL
}
