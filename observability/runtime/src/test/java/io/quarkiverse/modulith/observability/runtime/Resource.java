package io.quarkiverse.modulith.observability.runtime;

/**
 * Stands for a REST Data with Panache resource interface.
 */
interface Resource {

    void list(int page, String[] sort, Nested nested);
}
