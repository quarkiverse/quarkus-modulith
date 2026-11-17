package io.quarkiverse.modulith.events.mongodb.runtime;

import io.quarkiverse.modulith.events.core.EventSerializer;

/**
 * Reads strings back as they were written and counts how often it reads.
 */
public final class CountingSerializer implements EventSerializer {

    int reads;

    @Override
    public Object serialize(Object event) {
        return event;
    }

    @Override
    public <T> T deserialize(Object serialized, Class<T> type) {
        reads++;
        return type.cast(serialized);
    }
}
