package io.quarkiverse.modulith.events.jobrunr.runtime;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.externalization.EventExternalizer;
import io.quarkiverse.modulith.events.externalization.runtime.ExternalizationRuntimeConfig;
import io.quarkiverse.modulith.events.jobrunr.JobRunrExternalizationTransport;

/**
 * Sends the events of the jobs through the broker transport of the application, such as JMS or Quarkus Messaging, and
 * waits for the broker's acknowledgement, like the {@code JobRunrExternalizationTransport} beans Spring Modulith's
 * broker modules register. A failed send fails the job, and JobRunr retries it. So does a send the broker has not
 * acknowledged within {@code quarkus.modulith.events.externalization.outbox.send-timeout}, so a send that never
 * completes does not hold a JobRunr worker forever.
 */
@Singleton
public class EventExternalizerJobRunrTransport implements JobRunrExternalizationTransport {

    private final Function<Object, ? extends Future<?>> sender;
    private final Duration sendTimeout;

    @Inject
    public EventExternalizerJobRunrTransport(EventExternalizer externalizer, ExternalizationRuntimeConfig config) {
        this(externalizer::externalize, config.outbox().sendTimeout());
    }

    EventExternalizerJobRunrTransport(Function<Object, ? extends Future<?>> sender, Duration sendTimeout) {
        this.sender = sender;
        this.sendTimeout = sendTimeout;
    }

    @Override
    public void externalize(Object event) {
        String type = event.getClass().getName();
        try {
            sender.apply(event).get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while externalizing an event of type " + type, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Could not externalize an event of type " + type, e.getCause());
        } catch (TimeoutException e) {
            throw new IllegalStateException("The broker did not acknowledge an event of type " + type + " within "
                    + sendTimeout + " (quarkus.modulith.events.externalization.outbox.send-timeout)", e);
        }
    }
}
