package io.quarkiverse.modulith.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Published events backed by a list that the recorder may keep appending to while tests read it. Appending is cheap,
 * and every read works on a copy of the events recorded so far.
 */
class DefaultPublishedEvents implements PublishedEvents {

    private final List<Object> events;

    DefaultPublishedEvents() {
        this(List.of());
    }

    DefaultPublishedEvents(Collection<?> events) {
        this.events = new ArrayList<>(events);
    }

    /**
     * Records an event.
     */
    void add(Object event) {
        synchronized (events) {
            events.add(event);
        }
    }

    /**
     * Forgets all events.
     */
    void clear() {
        synchronized (events) {
            events.clear();
        }
    }

    @Override
    public <T> TypedPublishedEvents<T> ofType(Class<T> type) {
        return SimpleTypedPublishedEvents.of(snapshot().stream().filter(type::isInstance).map(type::cast));
    }

    @Override
    public String toString() {
        var recorded = snapshot();
        return recorded.isEmpty() ? "[]"
                : recorded.stream().map(String::valueOf).collect(Collectors.joining(", ", "[ ", " ]"));
    }

    private List<Object> snapshot() {
        synchronized (events) {
            return new ArrayList<>(events);
        }
    }
}
