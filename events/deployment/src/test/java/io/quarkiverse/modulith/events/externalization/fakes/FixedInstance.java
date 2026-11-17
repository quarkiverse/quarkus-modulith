package io.quarkiverse.modulith.events.externalization.fakes;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.Optional;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

public class FixedInstance<T> implements Instance<T> {

    private final Optional<T> value;

    public FixedInstance(Optional<T> value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value.orElseThrow();
    }

    @Override
    public boolean isUnsatisfied() {
        return value.isEmpty();
    }

    @Override
    public boolean isAmbiguous() {
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return value.stream().iterator();
    }

    @Override
    public Instance<T> select(Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroy(T instance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Handle<T> getHandle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<? extends Handle<T>> handles() {
        throw new UnsupportedOperationException();
    }
}
