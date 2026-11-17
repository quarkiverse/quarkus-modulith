package io.quarkiverse.modulith.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class PublishedEventsTest {

    private static final String NOT_EMPTY = "%nExpecting actual not to be empty".formatted();

    private final SampleCreated created = new SampleCreated("c-1");
    private final SampleDeleted deleted = new SampleDeleted("d-1");

    @Test
    void filtersByTypeAndSubtype() {
        // arrange
        PublishedEvents events = PublishedEvents.of(created, deleted, "text");

        // act
        TypedPublishedEvents<SampleEvent> samples = events.ofType(SampleEvent.class);

        // assert
        assertThat(samples).containsExactly(created, deleted);
        assertThat(samples.ofSubType(SampleDeleted.class)).containsExactly(deleted);
        assertThat(samples.ofType(String.class)).isEmpty();
        assertThat(events.eventOfTypeWasPublished(String.class)).isTrue();
        assertThat(samples.eventOfTypeWasPublished(Integer.class)).isFalse();
    }

    @Test
    void matchesByPredicateMappedValueAndNull() {
        // arrange
        TypedPublishedEvents<SampleEvent> samples = PublishedEvents.of(List.of(created, deleted, new SampleCreated(null)))
                .ofType(SampleEvent.class);

        // act
        TypedPublishedEvents<SampleEvent> byPredicate = samples.matching(it -> it instanceof SampleDeleted);

        // assert
        assertThat(byPredicate).containsExactly(deleted);
        assertThat(samples.matching(SampleEvent::id, "c-1"::equals)).containsExactly(created);
        assertThat(samples.matchingValue(SampleEvent::id, "d-1")).containsExactly(deleted);
        assertThat(samples.matchingValue(SampleEvent::id, null)).containsExactly(new SampleCreated(null));
    }

    @Test
    void describesItsEvents() {
        // arrange
        PublishedEvents events = PublishedEvents.of(created, deleted);

        // act
        String description = events.toString();

        // assert
        assertThat(description).isEqualTo("[ SampleCreated[id=c-1], SampleDeleted[id=d-1] ]");
        assertThat(PublishedEvents.of().toString()).isEqualTo("[]");
        assertThat(events.ofType(SampleCreated.class).toString()).isEqualTo("[SampleCreated[id=c-1]]");
    }

    @Test
    void recordsEvents() {
        // arrange
        DefaultPublishedEvents events = new DefaultPublishedEvents();

        // act
        events.add(created);

        // assert
        assertThat(events.ofType(Object.class)).containsExactly(created);
    }

    @Test
    void forgetsEvents() {
        // arrange
        DefaultPublishedEvents events = new DefaultPublishedEvents();
        events.add(created);

        // act
        events.clear();

        // assert
        assertThat(events.ofType(Object.class)).isEmpty();
    }

    @Test
    void keepsNullEventsOutOfTypedViews() {
        // arrange
        PublishedEvents events = PublishedEvents.of(created, null);

        // act
        TypedPublishedEvents<Object> all = events.ofType(Object.class);

        // assert
        assertThat(all).containsExactly(created);
        assertThat(events).hasToString("[ SampleCreated[id=c-1], null ]");
    }

    @Test
    void assertsOnEventsOfOneType() {
        // arrange
        DefaultPublishedEvents events = DefaultAssertablePublishedEvents.create();
        events.add(created);
        events.add(deleted);

        // act
        PublishedEventsAssert assertion = assertThat((AssertablePublishedEvents) events);

        // assert
        assertThat(assertion.contains(SampleCreated.class)
                .matching(it -> it.id().startsWith("c"))
                .matching(SampleCreated::id, "c-1"::equals)
                .matchingValue(SampleCreated::id, "c-1")
                .and()).isSameAs(assertion);
        assertion.contains(SampleDeleted.class).matchingValue(SampleDeleted::id, "d-1");
    }

    @Test
    void failsWhenNoEventMatches() {
        // arrange
        DefaultPublishedEvents events = DefaultAssertablePublishedEvents.create();
        events.add(created);
        PublishedEventsAssert assertion = assertThat((AssertablePublishedEvents) events);

        // act
        PublishedEventAssert<SampleCreated> ofType = assertion.contains(SampleCreated.class);

        // assert
        assertThatThrownBy(() -> assertion.contains(SampleDeleted.class)).isInstanceOf(AssertionError.class)
                .hasMessage(NOT_EMPTY);
        assertThatThrownBy(() -> ofType.matching(it -> false)).isInstanceOf(AssertionError.class)
                .hasMessage(NOT_EMPTY);
        assertThatThrownBy(() -> ofType.matching(SampleCreated::id, "x"::equals)).isInstanceOf(AssertionError.class)
                .hasMessage(NOT_EMPTY);
        assertThatThrownBy(() -> ofType.matchingValue(SampleCreated::id, "x")).isInstanceOf(AssertionError.class)
                .hasMessage(NOT_EMPTY);
    }
}
