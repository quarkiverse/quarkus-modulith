package io.quarkiverse.modulith.events.externalization.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkiverse.modulith.events.config.EventsConfig;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.PageCriteria;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.externalization.EventExternalizer;
import io.quarkiverse.modulith.events.spi.ListenerExecutor;
import io.quarkiverse.modulith.events.spi.SerializationFailures;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;

/**
 * Sends the outbox publications. Runs on a worker thread, one run at a time per instance, after each transaction that
 * recorded events, at startup and on a schedule. Each run sends the pending publications oldest first and waits for
 * each acknowledgement, so the broker sees them in order. A failed send stops the run, so that later events do not
 * overtake it, and the next run starts over with it.
 * <p>
 * Several instances may run at the same time: a publication is claimed with a conditional update before it is sent,
 * so only one instance sends it.
 * <p>
 * A publication whose event cannot be read (its type is gone, its JSON does not match) is marked failed with one more
 * attempt and skipped, so it never blocks the outbox. Publications at
 * {@code quarkus.modulith.events.resubmission.abandon-after-attempts} are skipped as well, with a warning, and are
 * listed by {@code AbandonedEventPublications}.
 */
@Singleton
public class OutboxDrain {

    private static final Logger LOG = Logger.getLogger(OutboxDrain.class);

    @Inject
    ExternalizationPublications publications;

    @Inject
    EventExternalizer externalizer;

    @Inject
    ExternalizationRuntimeConfig config;

    @Inject
    EventsConfig eventsConfig;

    @Inject
    ListenerExecutor executor;

    final AtomicInteger requests = new AtomicInteger();
    final ReentrantLock running = new ReentrantLock();
    final Set<UUID> reportedAbandoned = ConcurrentHashMap.newKeySet();
    Cancellable schedule;
    volatile boolean stopped;
    volatile UUID lastFailure;

    void start(@Observes StartupEvent startup) {
        validate(config.outbox());
        schedule = Multi.createFrom().ticks().every(config.outbox().interval())
                .subscribe().with(tick -> trigger());
        trigger();
    }

    /**
     * Stops taking runs and waits up to {@code send-timeout} for the current one, which stops after the publication it
     * is sending, so that no send meets a closed datasource. Runs before the datasources close.
     */
    void onShutdown(@Observes ShutdownEvent shutdown) {
        stopped = true;
        stop();
        awaitCurrentRun(config.outbox().sendTimeout());
    }

    @PreDestroy
    void stop() {
        if (Objects.nonNull(schedule)) {
            schedule.cancel();
        }
    }

    /**
     * @return whether no run was going on, or it ended within the timeout
     */
    boolean awaitCurrentRun(Duration timeout) {
        try {
            if (running.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                running.unlock();
                return true;
            }
            LOG.warnf("The outbox run did not end within %s at shutdown, the publication it is sending is sent again"
                    + " once stale", timeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    /**
     * Asks for a run. Returns right away, the run happens on a worker thread. Requests that arrive during a run cause
     * one more run. If the executor refuses the run, the request is dropped with an error, and the next trigger asks
     * again.
     */
    public void trigger() {
        if (stopped || requests.getAndIncrement() != 0) {
            return;
        }
        try {
            executor.execute(this::drain);
        } catch (RuntimeException e) {
            requests.set(0);
            LOG.errorf(e, "Could not start an outbox run, the next trigger tries again");
        }
    }

    /**
     * Runs until no request is left. Whatever ends it, also an {@link Error}, the requests are reset so that the next
     * trigger starts a new run.
     */
    void drain() {
        running.lock();
        boolean drained = false;
        try {
            int seen;
            do {
                seen = requests.get();
                runOnce();
            } while (requests.addAndGet(-seen) != 0);
            drained = true;
        } finally {
            if (!drained) {
                requests.set(0);
            }
            running.unlock();
        }
    }

    /**
     * Sends the pending publications page by page, oldest first. Stops at the first publication it cannot claim,
     * because another instance is sending it, at the first failed send, so that later events never overtake earlier
     * ones, and at shutdown.
     */
    void runOnce() {
        try {
            ExternalizationRuntimeConfig.Outbox outbox = config.outbox();
            Duration staleAfter = outbox.sendTimeout().plus(outbox.staleAfter());
            PageCriteria page = PageCriteria.first(EventPublicationRepository.FAR_FUTURE, outbox.batchSize());
            while (!stopped) {
                List<TargetEventPublication> pending = publications.findPending(OutboxEventExternalizer.LISTENER_ID,
                        page);
                for (TargetEventPublication publication : pending) {
                    if (stopped || !process(publication, staleAfter)) {
                        return;
                    }
                }
                if (pending.size() < outbox.batchSize()) {
                    return;
                }
                page = page.next(pending.get(pending.size() - 1));
            }
        } catch (RuntimeException e) {
            LOG.errorf(e, "The outbox run failed, the next run tries again");
        }
    }

    /**
     * Reads the event before claiming the publication, so that an unreadable one is failed and skipped.
     *
     * @return whether the run goes on with the next publication
     */
    boolean process(TargetEventPublication publication, Duration staleAfter) {
        UUID id = publication.getIdentifier();
        if (isAbandoned(publication)) {
            if (reportedAbandoned.add(id)) {
                LOG.warnf("Skipping outbox publication %s of type %s: it failed %d time(s), the abandon-after-attempts"
                        + " limit. Resubmit or delete it through AbandonedEventPublications", id,
                        publication.getEventType(), publication.getCompletionAttempts());
            }
            return true;
        }
        Optional<Object> event = read(publication);
        if (!claim(id, staleAfter)) {
            return false;
        }
        if (event.isEmpty()) {
            publications.markProcessing(id);
            publications.markFailed(id);
            return true;
        }
        return send(id, event.get());
    }

    /**
     * A serialization failure (SQLState {@code 40001}) means another instance claimed the publication concurrently.
     * Caught here, around the claim's own transaction, so that a failure reported at commit counts too.
     */
    private boolean claim(UUID id, Duration staleAfter) {
        try {
            return publications.claim(id, staleAfter);
        } catch (RuntimeException e) {
            if (SerializationFailures.isSerializationFailure(e)) {
                LOG.debugf("Outbox publication %s was claimed concurrently", id);
                return false;
            }
            throw e;
        }
    }

    private Optional<Object> read(TargetEventPublication publication) {
        try {
            return Optional.ofNullable(publication.getEvent());
        } catch (RuntimeException e) {
            logFailure(publication.getIdentifier(), publication.getEventType(), e);
            return Optional.empty();
        }
    }

    private boolean isAbandoned(TargetEventPublication publication) {
        return eventsConfig.resubmission().abandonAfterAttempts().stream()
                .anyMatch(limit -> publication.getCompletionAttempts() >= limit);
    }

    /**
     * Sends one claimed publication and records the outcome. A send that times out is cancelled, so that a late
     * acknowledgement is not reported as externalized.
     *
     * @return whether the broker acknowledged it
     */
    boolean send(UUID publicationId, Object event) {
        publications.markProcessing(publicationId);
        String eventType = event.getClass().getName();
        CompletableFuture<?> sent = externalizer.externalize(event);
        try {
            sent.get(config.outbox().sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
            publications.markCompleted(publicationId);
            return true;
        } catch (InterruptedException e) {
            try {
                publications.markFailed(publicationId);
            } finally {
                Thread.currentThread().interrupt();
            }
            return false;
        } catch (TimeoutException e) {
            sent.cancel(true);
            logFailure(publicationId, eventType, e);
        } catch (ExecutionException e) {
            logFailure(publicationId, eventType, Failures.unwrap(e));
        } catch (RuntimeException e) {
            logFailure(publicationId, eventType, e);
        }
        publications.markFailed(publicationId);
        return false;
    }

    /**
     * Logs a failed send at ERROR once per publication, and at DEBUG when the same publication fails again on the
     * following runs, so that a broker outage does not flood the log.
     */
    void logFailure(UUID publicationId, String eventType, Throwable failure) {
        if (publicationId.equals(lastFailure)) {
            LOG.debugf(failure, "Externalizing publication %s failed again", publicationId);
        } else {
            lastFailure = publicationId;
            LOG.errorf(failure, "Could not externalize an event of type %s (publication %s), retrying on the next"
                    + " runs", eventType, publicationId);
        }
    }

    /**
     * Fails the startup on outbox settings every run would fail on, naming the property.
     */
    static void validate(ExternalizationRuntimeConfig.Outbox outbox) {
        requirePositive("interval", outbox.interval());
        requirePositive("send-timeout", outbox.sendTimeout());
        if (outbox.staleAfter().isNegative()) {
            throw invalid("stale-after", "must not be negative", outbox.staleAfter());
        }
        if (outbox.batchSize() < 1) {
            throw invalid("batch-size", "must be at least 1", outbox.batchSize());
        }
    }

    private static void requirePositive(String property, Duration value) {
        if (value.isNegative() || value.isZero()) {
            throw invalid(property, "must be positive", value);
        }
    }

    private static IllegalArgumentException invalid(String property, String rule, Object value) {
        return new IllegalArgumentException(
                "quarkus.modulith.events.externalization.outbox." + property + " " + rule + ", got " + value);
    }
}
