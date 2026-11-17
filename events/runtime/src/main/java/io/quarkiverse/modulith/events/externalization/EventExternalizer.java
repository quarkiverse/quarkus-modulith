package io.quarkiverse.modulith.events.externalization;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkiverse.modulith.events.EventExternalizationConfiguration;
import io.quarkiverse.modulith.events.EventExternalized;
import io.quarkiverse.modulith.events.RoutingTarget;
import io.quarkiverse.modulith.events.externalization.runtime.ExternalizationRuntimeConfig;
import io.quarkiverse.modulith.events.externalization.runtime.RoutingResolver;
import io.quarkiverse.modulith.events.spi.ListenerExecutor;

/**
 * Externalizes one event: selects, routes, maps and sends it through the {@link EventExternalizationTransport}, then
 * fires {@link EventExternalized} with the broker's result. Like Spring Modulith's {@code EventExternalizerSupport} and
 * {@code TransportAwareEventExternalizer}.
 * <p>
 * Outbox implementations of other Quarkus Modulith extensions call it to send recorded events.
 */
@Singleton
public class EventExternalizer {

    private static final Logger LOG = Logger.getLogger(EventExternalizer.class);

    @Inject
    EventExternalizationConfiguration configuration;

    @Inject
    EventExternalizationTransport transport;

    @Inject
    RoutingResolver routing;

    /**
     * Typed with {@code Object} arguments, because CDI cannot fire an event whose type has unresolved type variables.
     * Observers declare {@code EventExternalized<?, ?>}.
     */
    @Inject
    Event<EventExternalized<Object, Object>> events;

    @Inject
    ListenerExecutor executor;

    @Inject
    ExternalizationRuntimeConfig runtimeConfig;

    /**
     * Completes, always normally, once the last serialized send completed. Each serialized send starts then.
     */
    final AtomicReference<CompletableFuture<?>> lastSerialized = new AtomicReference<>(
            CompletableFuture.completedFuture(null));

    /**
     * Sends the event if the configuration selects it. Never throws: failures complete the returned future
     * exceptionally. When the configuration serializes externalization, the send is queued behind the previous one
     * without blocking the caller, and fails when the broker does not acknowledge it within
     * {@code quarkus.modulith.events.externalization.serialized-send-timeout}, so that the next one can go.
     *
     * @return completes with the {@link EventExternalized} event once the broker acknowledged, with {@code null} when
     *         the event is not externalized
     */
    public CompletableFuture<EventExternalized<Object, Object>> externalize(Object event) {
        return externalize(event, reserve());
    }

    /**
     * Takes the next place in the order of serialized sends, for an event that is sent later, possibly on another
     * thread: the event is sent after the events of the places taken before, whenever {@link #externalize(Object, Turn)}
     * runs. A caller that takes a place in publication order thus gets the events to the broker in publication order.
     *
     * @return the place, which does not order anything when the configuration does not serialize externalization
     */
    public Turn reserve() {
        if (!configuration.serializeExternalization()) {
            return Turn.UNORDERED;
        }
        CompletableFuture<Void> turn = new CompletableFuture<>();
        return new Turn(lastSerialized.getAndSet(turn), turn);
    }

    /**
     * Like {@link #externalize(Object)}, for an event whose place in the order of serialized sends was taken with
     * {@link #reserve()}. The place is given up when the event is not sent, so the next event can go.
     */
    public CompletableFuture<EventExternalized<Object, Object>> externalize(Object event, Turn turn) {
        try {
            Objects.requireNonNull(event, "event");
            if (!configuration.supports(event)) {
                turn.skip();
                return CompletableFuture.completedFuture(null);
            }
            RoutingTarget target = routing.resolve(configuration.determineTarget(event), event);
            Object mapped = configuration.map(event);
            Map<String, Object> headers = configuration.getHeadersFor(mapped);
            LOG.debugf("Externalizing an event of type %s to %s", event.getClass().getName(), target);
            return turn.isOrdered() ? sendSerialized(turn, event, mapped, target, headers)
                    : send(event, mapped, target, headers);
        } catch (RuntimeException e) {
            turn.skip();
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Sends the event and waits for the broker's acknowledgement, like Spring Modulith's
     * {@code OutboxEventExternalizer#externalizeBlocking}.
     *
     * @throws IllegalStateException if sending failed or the thread was interrupted
     */
    public void externalizeBlocking(Object event) {
        try {
            externalize(event).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while externalizing an event of type "
                    + event.getClass().getName(), e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Could not externalize an event of type " + event.getClass().getName(),
                    e.getCause());
        }
    }

    private CompletableFuture<EventExternalized<Object, Object>> sendSerialized(Turn turn, Object event, Object mapped,
            RoutingTarget target, Map<String, Object> headers) {
        long timeout = runtimeConfig.serializedSendTimeout().toMillis();
        CompletableFuture<EventExternalized<Object, Object>> sent = turn.previous
                .thenCompose(previous -> send(event, mapped, target, headers).orTimeout(timeout, TimeUnit.MILLISECONDS));
        // the next send starts on a worker thread, also after a timeout, which completes on the JDK's timer thread
        sent.whenCompleteAsync((result, failure) -> turn.done.complete(null), executor::execute);
        return sent;
    }

    private CompletableFuture<EventExternalized<Object, Object>> send(Object event, Object mapped,
            RoutingTarget target, Map<String, Object> headers) {
        CompletionStage<?> sent;
        try {
            sent = transport.externalize(mapped, target, headers);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        if (Objects.isNull(sent)) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "The externalization transport " + transport.getClass().getName() + " returned null"));
        }
        // completes on a worker thread whatever the outcome, so that callers never record it on an I/O thread
        return sent.toCompletableFuture()
                .whenCompleteAsync(EventExternalizer::onWorker, executor::execute)
                .thenApply(result -> fire(new EventExternalized<>(event, mapped, target, result)));
    }

    private static void onWorker(Object result, Throwable failure) {
        // only moves the completion to the executor
    }

    private EventExternalized<Object, Object> fire(EventExternalized<Object, Object> externalized) {
        try {
            events.fire(externalized);
        } catch (RuntimeException e) {
            LOG.errorf(e, "An observer of EventExternalized failed for an event of type %s sent to %s",
                    externalized.getEventType().getName(), externalized.getTarget());
        }
        return externalized;
    }

    /**
     * A place in the order of serialized sends, see {@link #reserve()}.
     */
    public static final class Turn {

        /**
         * The place of a send that waits for no other send.
         */
        static final Turn UNORDERED = new Turn(null, null);

        /**
         * Completes, always normally, once the send of the place taken before completed.
         */
        final CompletableFuture<?> previous;

        /**
         * Completed once this place's send completed or was skipped, and the send of the previous place completed.
         */
        final CompletableFuture<Void> done;

        Turn(CompletableFuture<?> previous, CompletableFuture<Void> done) {
            this.previous = previous;
            this.done = done;
        }

        /**
         * @return whether the send waits for the send of the place taken before
         */
        public boolean isOrdered() {
            return Objects.nonNull(previous);
        }

        /**
         * Gives the place up without sending: the next place's send starts once the previous one completed.
         */
        public void skip() {
            if (isOrdered()) {
                previous.whenComplete((result, failure) -> done.complete(null));
            }
        }
    }
}
