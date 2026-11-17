package io.quarkiverse.modulith.events.externalization.fakes;

import java.lang.annotation.Annotation;
import java.util.Map;

import jakarta.enterprise.util.TypeLiteral;

import io.quarkus.arc.InterceptionProxy;
import io.quarkus.arc.SyntheticCreationalContext;

public class FakeCreationalContext<T> implements SyntheticCreationalContext<T> {

    private final Map<String, Object> params;
    private final Map<Class<?>, Object> references;

    public FakeCreationalContext(Map<String, Object> params, Map<Class<?>, Object> references) {
        this.params = params;
        this.references = references;
    }

    @Override
    public Map<String, Object> getParams() {
        return params;
    }

    @Override
    public <R> R getInjectedReference(Class<R> type, Annotation... qualifiers) {
        return type.cast(references.get(type));
    }

    @Override
    public <R> R getInjectedReference(TypeLiteral<R> type, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> InterceptionProxy<R> getInterceptionProxy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void push(T incompleteInstance) {
    }

    @Override
    public void release() {
    }
}
