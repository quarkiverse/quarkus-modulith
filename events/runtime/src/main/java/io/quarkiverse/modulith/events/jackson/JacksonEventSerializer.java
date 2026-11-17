package io.quarkiverse.modulith.events.jackson;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Reserve;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.core.EventSerializer;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serializes events to JSON with the application's Jackson 3 {@link JsonMapper}. A reserve bean,
 * so any other {@link EventSerializer} bean replaces it.
 */
@Singleton
@Reserve
@Priority(0)
public class JacksonEventSerializer implements EventSerializer {

    @Inject
    JsonMapper mapper;

    @Override
    public Object serialize(Object event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Cannot serialize event " + event.getClass().getName(), e);
        }
    }

    @Override
    public <T> T deserialize(Object serialized, Class<T> type) {
        try {
            return mapper.readValue(serialized.toString(), type);
        } catch (JacksonException e) {
            throw new IllegalStateException("Cannot read an event of type " + type.getName(), e);
        }
    }
}
