package io.quarkiverse.modulith.deployment.events;

import java.util.Objects;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.processor.ObserverTransformer;

/**
 * Removes the CDI observers the dispatcher delivers to instead, so that they are not notified twice.
 */
public final class RoutedObserverVeto implements ObserverTransformer {

    private final Set<String> routed;

    /**
     * @param routed the methods to veto, as {@link #key(MethodInfo)}
     */
    public RoutedObserverVeto(Set<String> routed) {
        this.routed = Set.copyOf(routed);
    }

    @Override
    public boolean appliesTo(Type observedType, Set<AnnotationInstance> qualifiers) {
        return true;
    }

    @Override
    public void transform(TransformationContext context) {
        // Synthetic observers have no method
        if (Objects.nonNull(context.getMethod()) && routed.contains(key(context.getMethod()))) {
            context.veto();
        }
    }

    public static String key(MethodInfo method) {
        return method.declaringClass().name() + "#" + method.name() + method.parameterTypes();
    }
}
