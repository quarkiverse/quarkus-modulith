package io.quarkiverse.modulith.events;

/**
 * Implemented by a bean with {@link io.quarkiverse.modulith.ApplicationModuleListener} methods that decides per event
 * whether its listeners apply, like Spring Modulith's {@code ConditionalEventListener}. The dispatcher asks before it
 * stores a publication, so events the bean does not support leave no trace in the event publication registry.
 */
public interface ConditionalEventListener {

    /**
     * @param event the published event
     * @return whether the listeners of this bean handle the event
     */
    boolean supports(Object event);
}
