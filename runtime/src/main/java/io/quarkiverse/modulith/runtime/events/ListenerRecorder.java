package io.quarkiverse.modulith.runtime.events;

import java.util.List;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

/**
 * Creates the {@link ListenerTable} bean from the listeners found at build time.
 */
@Recorder
public class ListenerRecorder {

    public Supplier<ListenerTable> listenerTable(List<ListenerData> listeners) {
        ListenerTable table = new ListenerTable(listeners);
        return () -> table;
    }
}
