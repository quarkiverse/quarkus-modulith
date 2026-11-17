package io.quarkiverse.modulith.test;

import java.util.Arrays;
import java.util.Collection;

/**
 * The events published while a test runs, the Quarkus counterpart of Spring Modulith's {@code PublishedEvents}.
 * <p>
 * Declare a parameter of this type in a test method of an {@link ApplicationModuleTest}, or register
 * {@link PublishedEventsExtension}, to get the events published during that test only:
 *
 * <pre>
 * &#64;Test
 * void publishesOrderCompleted(PublishedEvents events) {
 *     orders.complete("o-1");
 *     assertThat(events.ofType(OrderCompleted.class).matching(OrderCompleted::orderId, "o-1")).hasSize(1);
 * }
 * </pre>
 *
 * Events are recorded when the application runs in test mode and their type belongs to the application's base
 * package.
 */
public interface PublishedEvents extends TypedEvents {

    /**
     * @return published events made of the given events, for example to test code that collects events itself
     */
    static PublishedEvents of(Object... events) {
        return of(Arrays.asList(events));
    }

    /**
     * @return published events made of the given events
     */
    static PublishedEvents of(Collection<?> events) {
        return new DefaultPublishedEvents(events);
    }

    /**
     * @return every event the running test application recorded since it started or since
     *         {@link #clearCurrent()}, independent of the current test. Prefer a {@code PublishedEvents} test method
     *         parameter, which only sees the events of its test.
     * @throws IllegalStateException if no Quarkus application with Quarkus Modulith runs in test mode
     */
    static PublishedEvents current() {
        return PublishedEventsRecorder.current().all();
    }

    /**
     * Forgets the events returned by {@link #current()}.
     *
     * @throws IllegalStateException if no Quarkus application with Quarkus Modulith runs in test mode
     */
    static void clearCurrent() {
        PublishedEventsRecorder.current().clear();
    }

    @Override
    <T> TypedPublishedEvents<T> ofType(Class<T> type);
}
