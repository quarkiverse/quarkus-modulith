package io.quarkiverse.modulith.deployment.docs;

/**
 * The bean groups of the module canvases written by the build.
 */
public enum CanvasGroupings {

    /**
     * jMolecules groups, then REST resources, services, repositories, event listeners, configuration mappings,
     * scheduled jobs and messaging endpoints.
     */
    QUARKUS,

    /**
     * jMolecules groups, then Spring Modulith's controllers, services, repositories, event listeners and
     * configuration properties.
     */
    SPRING,

    /**
     * No groups, one list of beans.
     */
    NONE
}
