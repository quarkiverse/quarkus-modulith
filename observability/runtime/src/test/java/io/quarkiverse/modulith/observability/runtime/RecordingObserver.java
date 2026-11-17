package io.quarkiverse.modulith.observability.runtime;

import java.util.ArrayList;
import java.util.List;

import io.quarkiverse.modulith.observability.ModuleInvocation;

/**
 * Records what the interceptor asks it to observe.
 */
final class RecordingObserver implements InvocationObserver {

    final List<String> events = new ArrayList<>();

    @Override
    public ObservationScope start(ModuleInvocation invocation) {
        events.add("start " + invocation.method() + " in " + CurrentModule.get());
        return new RecordingScope(events);
    }
}
