package io.quarkiverse.modulith.runtime.events;

import java.util.Objects;

import jakarta.enterprise.context.Dependent;

import io.quarkiverse.modulith.events.ConditionalEventListener;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

/**
 * The listener bean a compiled condition asks through {@link ConditionalEventListener}. A bean with a context is
 * looked up once. A {@code @Dependent} bean gets a fresh instance per event, which is destroyed right after the call.
 */
public final class ConditionalBean {

    private final Class<?> beanClass;
    private volatile ConditionalEventListener contextual;

    public ConditionalBean(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    public boolean supports(Object event) {
        ConditionalEventListener cached = contextual;
        if (Objects.nonNull(cached)) {
            return cached.supports(event);
        }
        InstanceHandle<?> handle = Arc.container().instance(beanClass);
        if (handle.getBean().getScope() == Dependent.class) {
            try (handle) {
                return ((ConditionalEventListener) handle.get()).supports(event);
            }
        }
        contextual = (ConditionalEventListener) handle.get();
        return contextual.supports(event);
    }
}
