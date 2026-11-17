package io.quarkiverse.modulith.runtime.cdi;

import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;

/**
 * Hands out a fixed instance and records when it is destroyed.
 */
public final class FakeHandle<T> implements Instance.Handle<T> {

    private final Bean<T> bean;
    private final T instance;
    private final List<String> calls;

    public FakeHandle(Bean<T> bean, T instance, List<String> calls) {
        this.bean = bean;
        this.instance = instance;
        this.calls = calls;
    }

    @Override
    public T get() {
        return instance;
    }

    @Override
    public Bean<T> getBean() {
        return bean;
    }

    @Override
    public void destroy() {
        calls.add("destroy " + bean.getBeanClass().getSimpleName());
    }

    @Override
    public void close() {
        destroy();
    }
}
