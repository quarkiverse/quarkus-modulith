package io.quarkiverse.modulith.core;

import io.quarkiverse.modulith.core.data.EventSourceData;

/**
 * A method that creates an event instance.
 */
final class AccessSource implements Source {

    private final EventSourceData data;

    AccessSource(EventSourceData data) {
        this.data = data;
    }

    @Override
    public String toString(ApplicationModule module) {
        return "%s.%s(%s)".formatted(FormattableType.of(data.type()).getAbbreviatedFullName(module), data.method(),
                data.parameters() ? "…" : "");
    }

    @Override
    public String toString() {
        return data.type() + "." + data.method();
    }
}
