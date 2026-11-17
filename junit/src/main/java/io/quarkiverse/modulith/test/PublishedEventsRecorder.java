package io.quarkiverse.modulith.test;

import static java.util.Objects.isNull;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import org.jboss.logging.Logger;

import io.quarkiverse.modulith.core.ApplicationModules;
import io.quarkiverse.modulith.events.spi.PublishedEventsCapture;
import io.quarkiverse.modulith.testing.ModuleTestRuntimeConfig;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;

/**
 * Records the events of the application's base package while the application runs in test mode. Quarkus Modulith
 * registers this bean only in test mode, and only when {@code quarkus-modulith-junit} is on the class path, so the
 * production event path does not change.
 * <p>
 * Events fired through CDI reach it through its observers, which run before any other observer. Every recorded event
 * goes to the application-wide list behind {@link PublishedEvents#current()}, to every registered test, and to the
 * test bound to the publishing thread, if any.
 * <p>
 * The thread-bound test is kept in a holder that threads started by the test inherit. Unregistering the test empties
 * the holder, so pool threads started during the test stop recording into it once the test ended.
 */
@Singleton
class PublishedEventsRecorder implements PublishedEventsCapture {

    /**
     * The application-wide list stops growing at this size, so a long test run cannot exhaust the heap.
     */
    static final int MAX_EVENTS = 10_000;

    private static final Logger LOG = Logger.getLogger(PublishedEventsRecorder.class);

    private final String prefix;
    private final BooleanSupplier threadBoundByDefault;
    private final DefaultPublishedEvents all = new DefaultPublishedEvents();
    private final AtomicInteger recorded = new AtomicInteger();
    private final AtomicBoolean full = new AtomicBoolean();
    private final Set<DefaultPublishedEvents> registered = ConcurrentHashMap.newKeySet();
    private final InheritableThreadLocal<AtomicReference<DefaultPublishedEvents>> threadBound = new InheritableThreadLocal<>();

    /**
     * The runtime configuration is looked up lazily: the recorder also observes the events fired during static
     * initialization, before runtime configuration exists.
     */
    @Inject
    PublishedEventsRecorder(ApplicationModules modules, Instance<ModuleTestRuntimeConfig> config) {
        this(modules.getBasePackage(), () -> config.get().threadBoundPublishedEvents());
    }

    PublishedEventsRecorder(String basePackage, boolean threadBoundByDefault) {
        this(basePackage, () -> threadBoundByDefault);
    }

    /**
     * @param threadBoundByDefault {@code quarkus.modulith.test.thread-bound-published-events}
     */
    private PublishedEventsRecorder(String basePackage, BooleanSupplier threadBoundByDefault) {
        this.prefix = basePackage.isEmpty() ? "" : basePackage + ".";
        this.threadBoundByDefault = threadBoundByDefault;
    }

    /**
     * @return whether tests only see the events of their own thread and the threads it starts
     */
    boolean threadBoundByDefault() {
        return threadBoundByDefault.getAsBoolean();
    }

    /**
     * @return the recorder of the running test application
     * @throws IllegalStateException if no application with Quarkus Modulith runs in test mode
     */
    static PublishedEventsRecorder current() {
        return lookup(Arc.container());
    }

    static PublishedEventsRecorder lookup(ArcContainer container) {
        if (isNull(container)) {
            throw new IllegalStateException("No Quarkus application is running. Published events are only "
                    + "available in @QuarkusTest and @ApplicationModuleTest classes.");
        }
        InstanceHandle<PublishedEventsRecorder> handle = container.instance(PublishedEventsRecorder.class);
        if (!handle.isAvailable()) {
            throw new IllegalStateException("The running application does not record published events. Quarkus "
                    + "Modulith records them in test mode, when quarkus-modulith-junit is a test dependency.");
        }
        return handle.get();
    }

    void onEvent(@Observes @Priority(Interceptor.Priority.PLATFORM_BEFORE) Object event) {
        onPublished(event);
    }

    void onAsyncEvent(@ObservesAsync @Priority(Interceptor.Priority.PLATFORM_BEFORE) Object event) {
        onPublished(event);
    }

    @Override
    public void onPublished(Object event) {
        if (!event.getClass().getName().startsWith(prefix)) {
            return;
        }
        record(event);
        registered.forEach(events -> events.add(event));
        Optional.ofNullable(threadBound.get()).map(AtomicReference::get).ifPresent(events -> events.add(event));
    }

    private void record(Object event) {
        if (recorded.get() < MAX_EVENTS) {
            recorded.incrementAndGet();
            all.add(event);
        } else if (full.compareAndSet(false, true)) {
            LOG.warnf("Recorded %d events, PublishedEvents.current() ignores further events until "
                    + "PublishedEvents.clearCurrent() is called. Prefer a PublishedEvents test method parameter.",
                    MAX_EVENTS);
        }
    }

    /**
     * @return the events recorded since the start or the last {@link #clear()}, a live view
     */
    PublishedEvents all() {
        return all;
    }

    void clear() {
        all.clear();
        recorded.set(0);
        full.set(false);
    }

    /**
     * Starts recording into the given events.
     *
     * @param threadBoundOnly only record events published by the current thread and the threads it starts
     */
    <E extends DefaultPublishedEvents> E register(E events, boolean threadBoundOnly) {
        if (threadBoundOnly) {
            threadBound.set(new AtomicReference<>(events));
        } else {
            registered.add(events);
        }
        return events;
    }

    /**
     * Stops recording into the given events, also in the threads that inherited them.
     */
    void unregister(DefaultPublishedEvents events) {
        registered.remove(events);
        Optional.ofNullable(threadBound.get())
                .filter(holder -> holder.compareAndSet(events, null))
                .ifPresent(holder -> threadBound.remove());
    }
}
