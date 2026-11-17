package io.quarkiverse.modulith.core;

import java.util.List;

import io.quarkiverse.modulith.core.data.EventTypeData;

/**
 * An event type a module publishes, with the places that create it. Like Spring Modulith's {@code EventType}.
 */
public final class EventType {

    private final JavaType type;
    private final List<Source> sources;

    EventType(JavaType type, EventTypeData data) {
        this.type = type;
        this.sources = data.sources().stream().<Source> map(AccessSource::new).toList();
    }

    public JavaType getType() {
        return type;
    }

    /**
     * @return the constructor calls and static factory method calls that create the event outside its own type
     */
    public List<Source> getSources() {
        return sources;
    }

    public boolean hasSources() {
        return !sources.isEmpty();
    }

    @Override
    public String toString() {
        return "EventType [type=" + type + ", sources=" + sources + "]";
    }
}
