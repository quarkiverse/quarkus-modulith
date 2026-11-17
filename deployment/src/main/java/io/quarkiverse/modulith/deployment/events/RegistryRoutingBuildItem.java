package io.quarkiverse.modulith.deployment.events;

import java.util.Optional;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Produced when an event publication registry exists. Like Spring Modulith's registry, which gives every
 * {@code @TransactionalEventListener} a publication, CDI observers notified {@code AFTER_SUCCESS} are then routed
 * through the dispatcher so that their deliveries are stored, completed and resubmitted too.
 */
public final class RegistryRoutingBuildItem extends SimpleBuildItem {

    private final Optional<DotName> triggerAnnotation;

    /**
     * @param triggerAnnotation when present, only observer methods carrying this annotation are routed, like Spring's
     *        {@code spring.modulith.events.registry-trigger-annotation}
     */
    public RegistryRoutingBuildItem(Optional<DotName> triggerAnnotation) {
        this.triggerAnnotation = triggerAnnotation;
    }

    public Optional<DotName> getTriggerAnnotation() {
        return triggerAnnotation;
    }
}
