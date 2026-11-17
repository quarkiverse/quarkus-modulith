package io.quarkiverse.modulith.test;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Published events of one type. Spring Modulith declares it as {@code PublishedEvents.TypedPublishedEvents}.
 *
 * @param <T> the event type
 */
public interface TypedPublishedEvents<T> extends Iterable<T>, TypedEvents {

    /**
     * @return the events that are instances of the given subtype
     */
    default <S extends T> TypedPublishedEvents<S> ofSubType(Class<S> subType) {
        return ofType(subType);
    }

    @Override
    <S> TypedPublishedEvents<S> ofType(Class<S> type);

    /**
     * @return the events matching the predicate
     */
    TypedPublishedEvents<T> matching(Predicate<? super T> predicate);

    /**
     * @return the events whose value extracted by the mapper matches the predicate
     */
    <S> TypedPublishedEvents<T> matching(Function<T, S> mapper, Predicate<? super S> predicate);

    /**
     * @return the events whose value extracted by the mapper equals the given value, which may be {@code null}
     */
    <S> TypedPublishedEvents<T> matchingValue(Function<T, S> mapper, S value);
}
