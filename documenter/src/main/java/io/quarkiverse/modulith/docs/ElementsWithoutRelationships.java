package io.quarkiverse.modulith.docs;

/**
 * Whether diagrams show modules that have no relationship to another module in the diagram, Spring Modulith's
 * {@code DiagramOptions.ElementsWithoutRelationships}.
 */
public enum ElementsWithoutRelationships {

    /**
     * Modules without relationships are left out. The default.
     */
    HIDDEN,

    /**
     * Modules without relationships are shown.
     */
    VISIBLE
}
