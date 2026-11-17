package io.quarkiverse.modulith.events.core;

/**
 * Turns events into the form an event publication repository stores, and back.
 * <p>
 * The default implementation writes JSON with the application's Jackson mapper. Declare a CDI bean of this type to
 * replace it. The serialized form is an {@link Object} so that stores with a native document type can keep it as is,
 * like Spring Modulith's {@code EventSerializer}. The JSON implementation returns a {@link String}.
 */
public interface EventSerializer {

    /**
     * @param event the event to serialize, never {@code null}
     * @return the serialized event
     * @throws IllegalArgumentException if the event cannot be serialized
     */
    Object serialize(Object event);

    /**
     * @param serialized a value returned by {@link #serialize(Object)}
     * @param type the type of the event
     * @return the event
     * @throws IllegalStateException if the value cannot be read as the given type
     */
    <T> T deserialize(Object serialized, Class<T> type);
}
