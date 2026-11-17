package io.quarkiverse.modulith.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractAssert;

/**
 * AssertJ assertions for {@link PublishedEvents}.
 */
public class PublishedEventsAssert extends AbstractAssert<PublishedEventsAssert, TypedEvents> {

    PublishedEventsAssert(TypedEvents actual) {
        super(actual, PublishedEventsAssert.class);
    }

    /**
     * Asserts that at least one event of the given type was published.
     *
     * @return assertions on the events of that type
     */
    public <T> PublishedEventAssert<T> contains(Class<T> type) {
        TypedPublishedEvents<T> ofType = actual.ofType(type);
        assertThat(ofType).isNotEmpty();
        return new PublishedEventAssert<>(this, ofType);
    }
}
