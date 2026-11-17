package io.quarkiverse.modulith.test;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Resolves {@link PublishedEvents} and {@link AssertablePublishedEvents} test method parameters with the events
 * published while the test runs. All parameters of one test, and its {@link Scenario}, share one instance.
 * <p>
 * With {@code quarkus.modulith.test.thread-bound-published-events=true} a test only sees the events published by
 * its own thread and the threads that thread starts, so tests can run in parallel.
 */
class PublishedEventsParameterResolver implements ParameterResolver, AfterEachCallback {

    private static final Namespace NAMESPACE = Namespace.create(PublishedEventsParameterResolver.class);
    private static final String KEY = "events";

    private final Supplier<PublishedEventsRecorder> recorder;
    private final boolean assertJPresent;

    PublishedEventsParameterResolver() {
        this(PublishedEventsRecorder::current, ClassPresence.isPresent(ClassPresence.ASSERTJ));
    }

    PublishedEventsParameterResolver(Supplier<PublishedEventsRecorder> recorder, boolean assertJPresent) {
        this.recorder = recorder;
        this.assertJPresent = assertJPresent;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        if (type == AssertablePublishedEvents.class && !assertJPresent) {
            throw new ParameterResolutionException(
                    "The test method declares an AssertablePublishedEvents parameter, but AssertJ is not on the class path");
        }
        return PublishedEvents.class.isAssignableFrom(type);
    }

    @Override
    public PublishedEvents resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return resolve(extensionContext);
    }

    /**
     * @return the events of the current test, registered on first use
     */
    PublishedEvents resolve(ExtensionContext context) {
        return context.getStore(NAMESPACE).computeIfAbsent(KEY, k -> register(), EventsRegistration.class)
                .events();
    }

    EventsRegistration register() {
        PublishedEventsRecorder current = recorder.get();
        // Only touches the AssertJ-dependent type when AssertJ is present
        DefaultPublishedEvents events = assertJPresent ? DefaultAssertablePublishedEvents.create()
                : new DefaultPublishedEvents();
        return new EventsRegistration(current, current.register(events, current.threadBoundByDefault()));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Optional.ofNullable(context.getStore(NAMESPACE).remove(KEY, EventsRegistration.class))
                .ifPresent(EventsRegistration::close);
    }
}
