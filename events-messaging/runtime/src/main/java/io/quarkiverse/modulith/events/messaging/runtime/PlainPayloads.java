package io.quarkiverse.modulith.events.messaging.runtime;

import java.util.Optional;

import jakarta.inject.Singleton;

/**
 * Sends events as they are, so that the connector's serializer or converter writes them.
 */
@Singleton
public class PlainPayloads implements PayloadConverter {

    @Override
    public Object convert(Object payload) {
        return payload;
    }

    @Override
    public Optional<String> contentType() {
        return Optional.empty();
    }
}
