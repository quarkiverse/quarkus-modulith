package io.quarkiverse.modulith.runtime.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;

/**
 * A bean that only knows its class and scope.
 */
public record FakeBean<T>(Class<?> getBeanClass, Class<? extends Annotation> getScope) implements Bean<T> {

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Set.of();
    }

    @Override
    public T create(CreationalContext<T> creationalContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Type> getTypes() {
        return Set.of(getBeanClass);
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Set.of();
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Set.of();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }
}
