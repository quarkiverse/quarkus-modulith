package io.quarkiverse.modulith.test;

/**
 * The events recorded for one test, and the recorder to unregister them from when the test ends. JUnit also closes
 * it when the extension context it is stored in ends, for example the class context of a {@code PublishedEvents}
 * parameter of a {@code @BeforeAll} method.
 */
record EventsRegistration(PublishedEventsRecorder recorder, DefaultPublishedEvents events) implements AutoCloseable {

    @Override
    public void close() {
        recorder.unregister(events);
    }
}
