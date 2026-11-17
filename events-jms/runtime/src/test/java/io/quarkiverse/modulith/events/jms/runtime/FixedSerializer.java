package io.quarkiverse.modulith.events.jms.runtime;

import io.quarkiverse.modulith.events.core.EventSerializer;

public class FixedSerializer implements EventSerializer {

    @Override
    public Object serialize(Object event) {
        return new StringBuilder("{\"event\":\"").append(event).append("\"}");
    }

    @Override
    public <T> T deserialize(Object serialized, Class<T> type) {
        throw new UnsupportedOperationException("not read in these tests");
    }
}
