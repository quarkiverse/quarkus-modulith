package io.quarkiverse.modulith.observability.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import jakarta.interceptor.InvocationContext;

/**
 * An invocation of a method on a target, proceeding to a callable.
 */
final class FakeInvocationContext implements InvocationContext {

    private final Object target;
    private final Method method;
    private final Callable<Object> proceed;

    FakeInvocationContext(Object target, Method method, Callable<Object> proceed) {
        this.target = target;
        this.method = method;
        this.proceed = proceed;
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public Object getTimer() {
        return null;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Constructor<?> getConstructor() {
        return null;
    }

    @Override
    public Object[] getParameters() {
        return new Object[0];
    }

    @Override
    public void setParameters(Object[] params) {
    }

    @Override
    public Map<String, Object> getContextData() {
        return new HashMap<>();
    }

    @Override
    public Object proceed() throws Exception {
        return proceed.call();
    }
}
