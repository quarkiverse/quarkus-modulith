package io.quarkiverse.modulith.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

class PublishedEventsRecorderTest {

    private static final String NOT_RUNNING = "No Quarkus application is running. Published events are only available "
            + "in @QuarkusTest and @ApplicationModuleTest classes.";

    private final SampleCreated created = new SampleCreated("c-1");
    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    @AfterEach
    void stopPool() {
        pool.close();
    }

    static Stream<ThrowingCallable> entryPoints() {
        return Stream.of(() -> PublishedEventsRecorder.lookup(null), PublishedEventsRecorder::current,
                PublishedEvents::current, PublishedEvents::clearCurrent);
    }

    @Test
    void recordsOnlyEventsOfTheBasePackage() {
        // arrange
        PublishedEventsRecorder recorder = new PublishedEventsRecorder("io.quarkiverse.modulith", false);

        // act
        recorder.onEvent(created);
        recorder.onAsyncEvent("outside");

        // assert
        assertThat(recorder.all().ofType(Object.class)).containsExactly(created);
    }

    @Test
    void recordsEverythingWithoutBasePackage() {
        // arrange
        PublishedEventsRecorder recorder = new PublishedEventsRecorder("", false);

        // act
        recorder.onAsyncEvent("outside");

        // assert
        assertThat(recorder.all().ofType(Object.class)).containsExactly("outside");
    }

    @Test
    void forgetsTheApplicationWideEvents() {
        // arrange
        PublishedEventsRecorder recorder = new PublishedEventsRecorder("", false);
        recorder.onPublished("outside");

        // act
        recorder.clear();

        // assert
        assertThat(recorder.all().ofType(Object.class)).isEmpty();
    }

    @Test
    void stopsGrowingTheApplicationWideList() {
        // arrange
        PublishedEventsRecorder recorder = new PublishedEventsRecorder("", false);
        DefaultPublishedEvents test = recorder.register(new DefaultPublishedEvents(), false);

        // act
        IntStream.rangeClosed(0, PublishedEventsRecorder.MAX_EVENTS + 1).forEach(recorder::onPublished);

        // assert
        assertThat(recorder.all().ofType(Integer.class)).hasSize(10_000).last().isEqualTo(9_999);
        assertThat(test.ofType(Integer.class)).hasSize(10_002);
    }

    @Test
    void recordsAgainAfterClearingAFullList() {
        // arrange
        PublishedEventsRecorder recorder = new PublishedEventsRecorder("", false);
        IntStream.rangeClosed(0, PublishedEventsRecorder.MAX_EVENTS).forEach(recorder::onPublished);
        recorder.clear();

        // act
        recorder.onPublished(created);

        // assert
        assertThat(recorder.all().ofType(Object.class)).containsExactly(created);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void knowsWhetherTestsAreThreadBound(boolean configured) {
        // arrange
        PublishedEventsRecorder recorder = new PublishedEventsRecorder("", configured);

        // act
        boolean threadBound = recorder.threadBoundByDefault();

        // assert
        assertThat(threadBound).isEqualTo(configured);
    }

    @Test
    void stopsRecordingFromInheritingThreadsAfterUnregistering() throws Exception {
        // arrange
        PublishedEventsRecorder recorder = new PublishedEventsRecorder("io.quarkiverse.modulith", false);
        DefaultPublishedEvents bound = recorder.register(new DefaultPublishedEvents(), true);
        pool.submit(() -> recorder.onPublished(created)).get();
        recorder.unregister(bound);

        // act
        pool.submit(() -> recorder.onPublished(new SampleDeleted("d-1"))).get();

        // assert
        assertThat(bound.ofType(Object.class)).containsExactly(created);
    }

    @Test
    void keepsTheThreadBoundTestWhenAnotherOneUnregisters() {
        // arrange
        PublishedEventsRecorder recorder = new PublishedEventsRecorder("io.quarkiverse.modulith", false);
        DefaultPublishedEvents bound = recorder.register(new DefaultPublishedEvents(), true);
        recorder.unregister(new DefaultPublishedEvents());

        // act
        recorder.onPublished(created);

        // assert
        assertThat(bound.ofType(Object.class)).containsExactly(created);
    }

    @Test
    void sendsEventsToRegisteredTestsUntilTheyUnregister() {
        // arrange
        PublishedEventsRecorder recorder = new PublishedEventsRecorder("io.quarkiverse.modulith", false);
        DefaultPublishedEvents shared = recorder.register(new DefaultPublishedEvents(), false);
        DefaultPublishedEvents bound = recorder.register(new DefaultPublishedEvents(), true);

        // act
        recorder.onPublished(created);
        recorder.unregister(shared);
        recorder.unregister(new DefaultPublishedEvents());
        recorder.onPublished(new SampleDeleted("d-1"));
        recorder.unregister(bound);
        recorder.onPublished(new SampleDeleted("d-2"));

        // assert
        assertThat(shared.ofType(Object.class)).containsExactly(created);
        assertThat(bound.ofType(Object.class)).containsExactly(created, new SampleDeleted("d-1"));
    }

    @ParameterizedTest
    @MethodSource("entryPoints")
    void lookupNeedsARunningApplication(ThrowingCallable entryPoint) {
        // act
        Throwable failure = catchThrowable(entryPoint);

        // assert
        assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessage(NOT_RUNNING);
    }

    @Test
    @SuppressWarnings("unchecked")
    void lookupNeedsTheRecorderBean() {
        // arrange
        ArcContainer container = mock(ArcContainer.class);
        InstanceHandle<PublishedEventsRecorder> handle = mock(InstanceHandle.class);
        when(container.instance(PublishedEventsRecorder.class)).thenReturn(handle);

        // act
        Throwable failure = catchThrowable(() -> PublishedEventsRecorder.lookup(container));

        // assert
        assertThat(failure).isInstanceOf(IllegalStateException.class).hasMessage("The running application does not "
                + "record published events. Quarkus Modulith records them in test mode, when quarkus-modulith-junit is "
                + "a test dependency.");
    }

    @Test
    @SuppressWarnings("unchecked")
    void lookupReturnsTheRecorderBean() {
        // arrange
        PublishedEventsRecorder recorder = new PublishedEventsRecorder("", false);
        ArcContainer container = mock(ArcContainer.class);
        InstanceHandle<PublishedEventsRecorder> handle = mock(InstanceHandle.class);
        when(container.instance(PublishedEventsRecorder.class)).thenReturn(handle);
        when(handle.isAvailable()).thenReturn(true);
        when(handle.get()).thenReturn(recorder);

        // act
        PublishedEventsRecorder found = PublishedEventsRecorder.lookup(container);

        // assert
        assertThat(found).isSameAs(recorder);
    }
}
