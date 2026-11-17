package io.quarkiverse.modulith.events.deployment.externalization;

import java.util.Optional;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A concrete event type that carries a supported {@code Externalized} annotation, directly, on a supertype or as a
 * meta-annotation. Transports use it to prepare static routing, such as channels.
 */
public final class ExternalizedTypeBuildItem extends MultiBuildItem {

    private final String typeName;
    private final String declaration;
    private final String staticTarget;

    public ExternalizedTypeBuildItem(String typeName, String declaration, Optional<String> staticTarget) {
        this.typeName = typeName;
        this.declaration = declaration;
        this.staticTarget = staticTarget.orElse(null);
    }

    public String getTypeName() {
        return typeName;
    }

    /**
     * @return the routing target as declared in the annotation, possibly empty
     */
    public String getDeclaration() {
        return declaration;
    }

    /**
     * @return the target the default configuration routes the type to, empty when it is an expression
     */
    public Optional<String> getStaticTarget() {
        return Optional.ofNullable(staticTarget);
    }
}
