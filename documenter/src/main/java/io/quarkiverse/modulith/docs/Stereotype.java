package io.quarkiverse.modulith.docs;

/**
 * The architectural role of a bean or type, used to group the beans of a module canvas. The counterpart of Spring
 * Modulith's {@code ArchitecturallyEvidentType} checks ({@code isController()}, {@code isService()} and so on).
 */
public enum Stereotype {

    /**
     * A REST resource or web controller: a type with Jakarta REST {@code @Path} or a Vert.x {@code @Route} method.
     */
    CONTROLLER,

    /**
     * An application service: jMolecules {@code @Service} or a type named {@code *Service}.
     */
    SERVICE,

    /**
     * A repository: a Panache repository, a Jakarta Data repository or jMolecules {@code @Repository}.
     */
    REPOSITORY,

    /**
     * A bean with event listener methods.
     */
    EVENT_LISTENER,

    /**
     * A configuration mapping ({@code @ConfigMapping}) or a bean with {@code @ConfigProperty} injection points.
     */
    CONFIGURATION_PROPERTIES,

    /**
     * A bean with {@code @Scheduled} methods.
     */
    SCHEDULED_JOB,

    /**
     * A bean with Quarkus Messaging {@code @Incoming} or {@code @Outgoing} methods.
     */
    MESSAGING_ENDPOINT
}
