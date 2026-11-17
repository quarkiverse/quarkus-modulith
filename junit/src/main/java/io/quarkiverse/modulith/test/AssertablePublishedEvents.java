package io.quarkiverse.modulith.test;

import org.assertj.core.api.AssertProvider;

/**
 * {@link PublishedEvents} that AssertJ's {@code assertThat(…)} accepts:
 *
 * <pre>
 * assertThat(events)
 *         .contains(OrderCompleted.class)
 *         .matching(OrderCompleted::orderId, "o-1");
 * </pre>
 *
 * Needs AssertJ on the test class path.
 */
public interface AssertablePublishedEvents extends PublishedEvents, AssertProvider<PublishedEventsAssert> {

    @Override
    default PublishedEventsAssert assertThat() {
        return new PublishedEventsAssert(this);
    }
}
