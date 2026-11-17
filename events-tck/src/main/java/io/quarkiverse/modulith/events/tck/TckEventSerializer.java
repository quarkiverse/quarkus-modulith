package io.quarkiverse.modulith.events.tck;

import io.quarkiverse.modulith.events.core.EventSerializer;

/**
 * Writes the contract's events as compact JSON, {@code {"id":"<id>"}}, without a JSON library, for stores that keep
 * events as text, so their tests run the same on both Quarkus lines.
 */
public final class TckEventSerializer implements EventSerializer {

    private static final String PREFIX = "{\"id\":\"";
    private static final String SUFFIX = "\"}";

    @Override
    public Object serialize(Object event) {
        return PREFIX + ((TckEvent) event).id() + SUFFIX;
    }

    @Override
    public <T> T deserialize(Object serialized, Class<T> type) {
        String json = serialized.toString();
        return type.cast(new TckEvent(json.substring(PREFIX.length(), json.length() - SUFFIX.length())));
    }
}
