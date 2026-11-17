package io.quarkiverse.modulith.it.moduletests.threadbound;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkiverse.modulith.it.moduletests.order.OrderCancelled;
import io.quarkiverse.modulith.test.PublishedEvents;
import io.quarkiverse.modulith.test.PublishedEventsExtension;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(ThreadBoundProfile.class)
@ExtendWith(PublishedEventsExtension.class)
class ThreadBoundPublishedEventsTest {

    private static ExecutorService otherThread;

    @Inject
    Event<Object> events;

    @BeforeAll
    static void startAThreadBeforeTheTest() throws Exception {
        otherThread = Executors.newSingleThreadExecutor();
        otherThread.submit(() -> {
        }).get();
    }

    @AfterAll
    static void stopTheThread() {
        otherThread.shutdownNow();
    }

    @Test
    void seesOnlyItsOwnThreadAndItsChildren(PublishedEvents published) throws Exception {
        // arrange
        Thread child = new Thread(() -> events.fire(new OrderCancelled("child")));

        // act
        events.fire(new OrderCancelled("own"));
        child.start();
        child.join();
        otherThread.submit(() -> events.fire(new OrderCancelled("other"))).get();

        // assert
        assertThat(published.ofType(OrderCancelled.class))
                .containsExactly(new OrderCancelled("own"), new OrderCancelled("child"));
        assertThat(PublishedEvents.current().ofType(OrderCancelled.class)).contains(new OrderCancelled("other"));
    }
}
