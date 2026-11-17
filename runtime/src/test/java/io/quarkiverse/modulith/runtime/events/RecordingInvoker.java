package io.quarkiverse.modulith.runtime.events;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.invoke.Invoker;

/**
 * Records the events it is called with, or fails the way it is told to.
 */
final class RecordingInvoker implements Invoker<Object, Object> {

    private final List<Object> calls;
    private final Exception failure;

    RecordingInvoker(List<Object> calls, Exception failure) {
        this.calls = calls;
        this.failure = failure;
    }

    @Override
    public Object invoke(Object instance, Object[] arguments) throws Exception {
        calls.add(arguments[0]);
        if (Objects.nonNull(failure)) {
            throw failure;
        }
        return null;
    }
}
