package io.quarkiverse.modulith.runtime.cdi;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

/**
 * An {@link Instance} over a fixed list of beans.
 */
public final class FakeInstance<T> implements Instance<T> {

    private final List<Instance.Handle<T>> handles;

    public FakeInstance(List<Instance.Handle<T>> handles) {
        this.handles = List.copyOf(handles);
    }

    @Override
    public T get() {
        return getHandle().get();
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
    public boolean isUnsatisfied() {
        return handles.isEmpty();
    }

    @Override
    public boolean isAmbiguous() {
        return handles.size() > 1;
    }

    @Override
    public void destroy(T instance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Instance.Handle<T> getHandle() {
        return handles.getFirst();
    }

    @Override
    public Iterable<? extends Instance.Handle<T>> handles() {
        return handles;
    }

    @Override
    public Iterator<T> iterator() {
        return handles.stream().map(Instance.Handle::get).iterator();
    }
}
