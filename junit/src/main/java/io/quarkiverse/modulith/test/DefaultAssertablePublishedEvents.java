package io.quarkiverse.modulith.test;

/**
 * Recorded events that AssertJ can assert on. Kept apart from {@link DefaultPublishedEvents} so that AssertJ stays
 * optional.
 */
final class DefaultAssertablePublishedEvents extends DefaultPublishedEvents implements AssertablePublishedEvents {

    /**
     * Declared as the superclass, so that callers do not load this class unless they call the method.
     */
    static DefaultPublishedEvents create() {
        return new DefaultAssertablePublishedEvents();
    }
}
