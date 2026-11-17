package io.quarkiverse.modulith.it.eventsmongodb.repository;

import io.quarkiverse.modulith.events.core.EventSerializer;
import io.quarkiverse.modulith.events.tck.TckEvent;

/**
 * Writes events as plain text instead of JSON objects.
 */
public final class TextSerializer implements EventSerializer {

    @Override
    public Object serialize(Object event) {
        return "tck:" + ((TckEvent) event).id();
    }

    @Override
    public <T> T deserialize(Object serialized, Class<T> type) {
        return type.cast(new TckEvent(serialized.toString().substring("tck:".length())));
    }
}
