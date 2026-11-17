package io.quarkiverse.modulith.events.messaging.runtime;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.core.EventSerializer;

/**
 * Sends events as JSON written by the {@link EventSerializer}, like Spring Modulith with
 * {@code spring.modulith.events.kafka.enable-json} and {@code spring.modulith.events.rabbitmq.enable-json}.
 */
@Singleton
public class JsonPayloads implements PayloadConverter {

    static final String JSON = "application/json";

    @Inject
    EventSerializer serializer;

    @Override
    public Object convert(Object payload) {
        return serializer.serialize(payload);
    }

    @Override
    public Optional<String> contentType() {
        return Optional.of(JSON);
    }
}
