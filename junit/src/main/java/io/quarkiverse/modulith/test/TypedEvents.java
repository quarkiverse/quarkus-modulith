package io.quarkiverse.modulith.test;

/**
 * Events that can be narrowed down by type, the common view of {@link PublishedEvents} and
 * {@link TypedPublishedEvents}.
 */
public interface TypedEvents {

    /**
     * @return the events that are instances of the given type, never {@code null}
     */
    <T> TypedPublishedEvents<T> ofType(Class<T> type);

    /**
     * @return whether at least one event of the given type was published
     */
    default boolean eventOfTypeWasPublished(Class<?> type) {
        return ofType(type).iterator().hasNext();
    }
}
