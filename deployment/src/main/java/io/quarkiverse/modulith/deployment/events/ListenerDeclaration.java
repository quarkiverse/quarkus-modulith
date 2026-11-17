package io.quarkiverse.modulith.deployment.events;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.MethodInfo;

/**
 * A listener method and the {@code @ApplicationModuleListener} that applies to it, either on the method itself or on
 * the composed annotation the method carries.
 *
 * @param method the listener method
 * @param attributes the annotation whose attributes apply, {@code null} for a routed CDI observer, which has the
 *        defaults of a synchronous listener without transaction
 * @param routedObserver whether the method is a CDI observer notified {@code AFTER_SUCCESS} that the dispatcher
 *        delivers to because an event publication registry exists
 */
public record ListenerDeclaration(MethodInfo method, AnnotationInstance attributes, boolean routedObserver) {
}
