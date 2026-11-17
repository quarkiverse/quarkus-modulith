package io.quarkiverse.modulith.events.externalization.runtime;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkiverse.modulith.events.EventExternalizationConfiguration;
import io.quarkiverse.modulith.events.externalization.EventExternalizer;
import io.quarkiverse.modulith.events.externalization.EventExternalizer.Turn;
import io.quarkiverse.modulith.events.externalization.ExternalizationStrategy;
import io.quarkiverse.modulith.events.spi.ListenerExecutor;
import io.quarkus.runtime.BlockingOperationControl;

/**
 * Externalizes events like an {@code @ApplicationModuleListener}, the default mode and Spring Modulith's
 * {@code EventExternalizerModuleListener}: the event publication is stored in the publishing transaction, the event is
 * sent on a worker thread after the commit, and the publication completes when the broker acknowledged it. A failed send
 * leaves the publication incomplete, so that the event publication registry can resubmit it.
 */
@Singleton
public class EventExternalizerModuleListener implements ExternalizationStrategy {

    /**
     * The listener identifier stored with the publications, in the format of {@code @ApplicationModuleListener} ids.
     */
    public static final String LISTENER_ID = "io.quarkiverse.modulith.events.externalization.runtime."
            + "EventExternalizerModuleListener.externalize(java.lang.Object)";

    /**
     * The identifier Spring Modulith stores, so that publications of an application that moved from Spring Modulith
     * are resubmitted too.
     */
    public static final String SPRING_LISTENER_ID = "org.springframework.modulith.events.support."
            + "EventExternalizerModuleListener.externalize(java.lang.Object)";

    private static final Logger LOG = Logger.getLogger(EventExternalizerModuleListener.class);
    private static final Set<String> LISTENER_IDS = Set.of(LISTENER_ID, SPRING_LISTENER_ID);

    @Inject
    EventExternalizationConfiguration configuration;

    @Inject
    EventExternalizer externalizer;

    @Inject
    ExternalizationPublications publications;

    @Inject
    ExternalizationTransactions transactions;

    @Inject
    ListenerExecutor executor;

    /**
     * Whether the last send failed, so that an outage logs one error and not one per event.
     */
    final AtomicBoolean failing = new AtomicBoolean();

    @Override
    public void onEvent(Object event) {
        if (!configuration.supports(event)) {
            return;
        }
        if (transactions.isActive()) {
            Optional<UUID> publication = publications.store(event, LISTENER_ID);
            transactions.afterCommit(() -> sendLater(event, () -> publication));
        } else if (BlockingOperationControl.isBlockingAllowed()) {
            Optional<UUID> publication = publications.store(event, LISTENER_ID);
            sendLater(event, () -> publication);
        } else {
            sendLater(event, () -> publications.store(event, LISTENER_ID));
        }
    }

    @Override
    public boolean handles(String listenerId) {
        return LISTENER_IDS.contains(listenerId);
    }

    @Override
    public void resubmit(UUID publicationId, Object event) {
        sendLater(event, () -> Optional.of(publicationId));
    }

    /**
     * Takes the event's place in the order of serialized sends on the calling thread, which sees the events in
     * publication order (after each commit, or as they are published without a transaction), then sends it on a worker
     * thread. Worker threads run in any order, so the place cannot be taken there.
     */
    private void sendLater(Object event, Supplier<Optional<UUID>> publication) {
        Turn turn = externalizer.reserve();
        try {
            executor.execute(() -> send(event, publication, turn));
        } catch (RuntimeException e) {
            turn.skip();
            throw e;
        }
    }

    /**
     * Sends the event in its place and records the outcome. Runs on a worker thread.
     */
    private void send(Object event, Supplier<Optional<UUID>> stored, Turn turn) {
        Optional<UUID> publication;
        try {
            publication = stored.get();
        } catch (RuntimeException e) {
            turn.skip();
            throw e;
        }
        try {
            publication.ifPresent(publications::markProcessing);
        } catch (RuntimeException e) {
            turn.skip();
            LOG.errorf(e, "Could not mark event publication %s as processing", publication.orElseThrow());
            return;
        }
        externalizer.externalize(event, turn).whenComplete((result, failure) -> record(event, publication, failure));
    }

    private void record(Object event, Optional<UUID> publication, Throwable failure) {
        String id = publication.map(UUID::toString).orElse("not stored");
        try {
            if (Objects.isNull(failure)) {
                if (failing.compareAndSet(true, false)) {
                    LOG.info("Externalizing events works again");
                }
                publication.ifPresent(publications::markCompleted);
            } else {
                logFailure(event, id, Failures.unwrap(failure));
                publication.ifPresent(publications::markFailed);
            }
        } catch (RuntimeException e) {
            LOG.errorf(e, "Could not record the outcome of event publication %s", id);
        }
    }

    /**
     * Logs the first failed send at ERROR, and the following ones at DEBUG until a send succeeds again.
     */
    private void logFailure(Object event, String id, Throwable failure) {
        if (failing.compareAndSet(false, true)) {
            LOG.errorf(failure, "Could not externalize an event of type %s (publication %s), logging further failures"
                    + " at DEBUG until a send succeeds", event.getClass().getName(), id);
        } else {
            LOG.debugf(failure, "Could not externalize an event of type %s (publication %s)",
                    event.getClass().getName(), id);
        }
    }
}
