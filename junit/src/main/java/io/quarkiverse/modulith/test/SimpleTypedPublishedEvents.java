package io.quarkiverse.modulith.test;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * An immutable list of events of one type.
 */
final class SimpleTypedPublishedEvents<T> implements TypedPublishedEvents<T> {

    private final List<T> events;

    private SimpleTypedPublishedEvents(List<T> events) {
        this.events = events;
    }

    static <T> SimpleTypedPublishedEvents<T> of(Stream<T> stream) {
        return new SimpleTypedPublishedEvents<>(stream.toList());
    }

    @Override
    public <S> TypedPublishedEvents<S> ofType(Class<S> type) {
        return of(events.stream().filter(type::isInstance).map(type::cast));
    }

    @Override
    public TypedPublishedEvents<T> matching(Predicate<? super T> predicate) {
        return of(events.stream().filter(predicate));
    }

    @Override
    public <S> TypedPublishedEvents<T> matching(Function<T, S> mapper, Predicate<? super S> predicate) {
        return of(events.stream().filter(it -> predicate.test(mapper.apply(it))));
    }

    @Override
    public <S> TypedPublishedEvents<T> matchingValue(Function<T, S> mapper, S value) {
        return matching(mapper, it -> Objects.equals(it, value));
    }

    @Override
    public Iterator<T> iterator() {
        return events.iterator();
    }

    @Override
    public String toString() {
        return events.toString();
    }
}
