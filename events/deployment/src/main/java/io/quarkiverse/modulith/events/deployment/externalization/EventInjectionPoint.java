package io.quarkiverse.modulith.events.deployment.externalization;

import org.jboss.jandex.Type;

/**
 * An injection point of a bean, reduced to what decides the observed event types.
 *
 * @param requiredType the injected type, for example {@code Event<OrderPlaced>}
 * @param beanClass the class of the bean declaring it, empty when unknown
 */
record EventInjectionPoint(Type requiredType, String beanClass) {
}
