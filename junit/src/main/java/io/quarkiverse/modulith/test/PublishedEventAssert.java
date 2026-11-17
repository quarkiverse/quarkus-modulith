package io.quarkiverse.modulith.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Assertions on the published events of one type. Spring Modulith declares it as
 * {@code PublishedEventsAssert.PublishedEventAssert}.
 *
 * @param <T> the event type
 */
public class PublishedEventAssert<T> {

    private final PublishedEventsAssert parent;
    private final TypedPublishedEvents<T> events;

    PublishedEventAssert(PublishedEventsAssert parent, TypedPublishedEvents<T> events) {
        this.parent = parent;
        this.events = events;
    }

    /**
     * Asserts that at least one event matches the predicate.
     */
    public PublishedEventAssert<T> matching(Predicate<? super T> predicate) {
        assertThat(events.matching(predicate)).isNotEmpty();
        return this;
    }

    /**
     * Asserts that the value extracted from at least one event matches the predicate.
     */
    public <S> PublishedEventAssert<T> matching(Function<T, S> function, Predicate<? super S> predicate) {
        assertThat(events.matching(function, predicate)).isNotEmpty();
        return this;
    }

    /**
     * Asserts that the value extracted from at least one event equals the given value.
     */
    public <S> PublishedEventAssert<T> matchingValue(Function<T, S> function, S value) {
        assertThat(events.matchingValue(function, value)).isNotEmpty();
        return this;
    }

    /**
     * @return the assertions on all published events, to continue with another event type
     */
    public PublishedEventsAssert and() {
        return parent;
    }
}
