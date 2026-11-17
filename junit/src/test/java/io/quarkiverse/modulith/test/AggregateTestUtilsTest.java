package io.quarkiverse.modulith.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AggregateTestUtilsTest {

    static Stream<Aggregate> aggregates() {
        return Stream.of(new Aggregate(), new DerivedAggregate());
    }

    @ParameterizedTest
    @MethodSource("aggregates")
    void readsTheDomainEventsOfTheAggregateAndItsSuperclasses(Aggregate aggregate) {
        // act
        PublishedEvents events = AggregateTestUtils.eventsOf(aggregate);

        // assert
        assertThat(events.ofType(SampleEvent.class)).containsExactly(new SampleCreated("c-1"),
                new SampleDeleted("d-1"));
        assertThat(events.eventOfTypeWasPublished(String.class)).isFalse();
    }

    @Test
    void hasNoEventsWithoutAnnotatedMethod() {
        // act
        PublishedEvents events = AggregateTestUtils.eventsOf("plain");

        // assert
        assertThat(events.ofType(Object.class)).isEmpty();
    }

    @Test
    void reportsFailingMethods() {
        // act
        Throwable failure = catchThrowable(() -> AggregateTestUtils.eventsOf(new BrokenAggregate()));

        // assert
        assertThat(failure).isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot read the domain events of io.quarkiverse.modulith.test.BrokenAggregate")
                .rootCause().hasMessage("broken");
    }

    @Test
    void needsAnAggregate() {
        // act
        Throwable failure = catchThrowable(() -> AggregateTestUtils.eventsOf(null));

        // assert
        assertThat(failure).isInstanceOf(NullPointerException.class).hasMessage("aggregate");
    }
}
