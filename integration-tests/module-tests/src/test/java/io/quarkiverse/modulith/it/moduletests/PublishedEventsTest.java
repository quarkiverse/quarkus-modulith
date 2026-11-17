package io.quarkiverse.modulith.it.moduletests;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkiverse.modulith.it.moduletests.order.OrderCancelled;
import io.quarkiverse.modulith.it.moduletests.order.OrderPlaced;
import io.quarkiverse.modulith.test.AssertablePublishedEvents;
import io.quarkiverse.modulith.test.PublishedEvents;
import io.quarkiverse.modulith.test.PublishedEventsExtension;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@ExtendWith(PublishedEventsExtension.class)
class PublishedEventsTest {

    @Inject
    Event<Object> events;

    @BeforeEach
    void forgetEarlierEvents() {
        PublishedEvents.clearCurrent();
    }

    @Test
    void parameterSeesTheEventsOfItsTest(PublishedEvents published) {
        // act
        events.fire(new OrderCancelled("o-e1"));

        // assert
        assertThat(published.ofType(OrderCancelled.class)).containsExactly(new OrderCancelled("o-e1"));
        assertThat(published.eventOfTypeWasPublished(OrderPlaced.class)).isFalse();
    }

    @Test
    void assertableParameterIsTheSameInstance(PublishedEvents published, AssertablePublishedEvents assertable) {
        // act
        events.fire(new OrderCancelled("o-e2"));

        // assert
        assertThat(assertable).isSameAs(published);
        assertThat(assertable)
                .contains(OrderCancelled.class)
                .matching(event -> event.orderId().equals("o-e2"))
                .matching(OrderCancelled::orderId, id -> id.startsWith("o-"))
                .matchingValue(OrderCancelled::orderId, "o-e2");
    }

    @Test
    void recordsAsynchronousEvents(PublishedEvents published) {
        // act
        events.fireAsync(new OrderCancelled("o-e3")).toCompletableFuture().join();

        // assert
        assertThat(published.ofType(OrderCancelled.class)).containsExactly(new OrderCancelled("o-e3"));
    }

    @Test
    void ignoresEventsOutsideTheBasePackage(PublishedEvents published) {
        // act
        events.fire("not an application event");

        // assert
        assertThat(published.ofType(Object.class)).isEmpty();
        assertThat(PublishedEvents.current().ofType(Object.class)).isEmpty();
    }

    @Test
    void clearCurrentForgetsTheRecordedEvents() {
        // arrange
        events.fire(new OrderCancelled("o-e4"));

        // act
        PublishedEvents.clearCurrent();

        // assert
        assertThat(PublishedEvents.current().eventOfTypeWasPublished(OrderCancelled.class)).isFalse();
    }

    @Test
    void currentRecordsEventsFiredAfterAClear() {
        // arrange
        events.fire(new OrderCancelled("o-e4"));
        PublishedEvents.clearCurrent();

        // act
        events.fire(new OrderCancelled("o-e5"));

        // assert
        assertThat(PublishedEvents.current().ofType(OrderCancelled.class)).containsExactly(new OrderCancelled("o-e5"));
    }
}
