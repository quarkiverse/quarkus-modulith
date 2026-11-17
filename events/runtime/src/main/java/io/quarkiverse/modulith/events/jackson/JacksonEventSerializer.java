package io.quarkiverse.modulith.events.jackson;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.modulith.events.core.EventSerializer;
import io.quarkus.arc.DefaultBean;

/**
 * Serializes events to JSON with the application's Jackson 2 {@link ObjectMapper}.
 */
@Singleton
@DefaultBean
public class JacksonEventSerializer implements EventSerializer {

    @Inject
    ObjectMapper mapper;

    @Override
    public Object serialize(Object event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize event " + event.getClass().getName(), e);
        }
    }

    @Override
    public <T> T deserialize(Object serialized, Class<T> type) {
        try {
            return mapper.readValue(serialized.toString(), type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read an event of type " + type.getName(), e);
        }
    }
}
