package io.quarkiverse.modulith.core;

import java.util.stream.Stream;

/**
 * The architectural role of a type, derived from CDI, Jakarta, Quarkus, Spring and jMolecules annotations and types.
 * Like Spring Modulith's {@code ArchitecturallyEvidentType}.
 */
public final class ArchitecturallyEvidentType {

    public static final String ENTITY = "ENTITY";
    public static final String AGGREGATE_ROOT = "AGGREGATE_ROOT";
    public static final String REPOSITORY = "REPOSITORY";
    public static final String SERVICE = "SERVICE";
    public static final String CONTROLLER = "CONTROLLER";
    public static final String EVENT_LISTENER = "EVENT_LISTENER";
    public static final String CONFIGURATION_PROPERTIES = "CONFIGURATION_PROPERTIES";
    public static final String INJECTABLE = "INJECTABLE";
    public static final String VALUE_OBJECT = "VALUE_OBJECT";

    private final JavaType type;

    ArchitecturallyEvidentType(JavaType type) {
        this.type = type;
    }

    /**
     * @return the roles of the given type
     */
    public static ArchitecturallyEvidentType of(JavaType type) {
        return new ArchitecturallyEvidentType(type);
    }

    public JavaType getType() {
        return type;
    }

    /**
     * @return whether the type is a JPA, MongoDB or jMolecules entity
     */
    public boolean isEntity() {
        return type.hasStereotype(ENTITY);
    }

    public boolean isAggregateRoot() {
        return type.hasStereotype(AGGREGATE_ROOT);
    }

    public boolean isRepository() {
        return type.hasStereotype(REPOSITORY);
    }

    public boolean isService() {
        return type.hasStereotype(SERVICE);
    }

    /**
     * @return whether the type handles web requests, for example a Jakarta REST resource
     */
    public boolean isController() {
        return type.hasStereotype(CONTROLLER);
    }

    public boolean isEventListener() {
        return type.hasStereotype(EVENT_LISTENER);
    }

    /**
     * @return whether the type holds configuration, for example a {@code @ConfigMapping} interface
     */
    public boolean isConfigurationProperties() {
        return type.hasStereotype(CONFIGURATION_PROPERTIES);
    }

    /**
     * @return whether the type is a bean other beans may depend on
     */
    public boolean isInjectable() {
        return type.hasStereotype(INJECTABLE);
    }

    public boolean isValueObject() {
        return type.hasStereotype(VALUE_OBJECT);
    }

    /**
     * @return the methods receiving events, for event listeners
     */
    public Stream<ReferenceMethod> getReferenceMethods() {
        return type.getData().referenceMethods().stream().map(it -> new ReferenceMethod(type, it));
    }

    @Override
    public String toString() {
        return type.getName();
    }
}
