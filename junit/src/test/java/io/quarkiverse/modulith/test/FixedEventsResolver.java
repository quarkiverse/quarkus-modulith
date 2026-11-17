package io.quarkiverse.modulith.test;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Returns the same events for every test, without a JUnit store.
 */
class FixedEventsResolver extends PublishedEventsParameterResolver {

    final DefaultPublishedEvents events = DefaultAssertablePublishedEvents.create();

    FixedEventsResolver() {
        super(() -> new PublishedEventsRecorder("", false), true);
    }

    @Override
    PublishedEvents resolve(ExtensionContext context) {
        return events;
    }
}
