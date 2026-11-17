package io.quarkiverse.modulith.runtime.events;

import java.lang.annotation.Annotation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.enterprise.invoke.Invoker;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkiverse.modulith.events.spi.EventPublicationRegistry;
import io.quarkiverse.modulith.events.spi.EventRedelivery;
import io.quarkiverse.modulith.events.spi.ListenerExecutor;
import io.quarkiverse.modulith.events.spi.ReadOnlyTransactions;
import io.quarkiverse.modulith.events.spi.SerializationFailures;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.ShutdownEvent;

/**
 * Routes CDI events to {@link io.quarkiverse.modulith.ApplicationModuleListener} methods.
 * <p>
 * The deployment module registers one synthetic observer per event type a listener listens to, so the dispatcher only
 * sees events it routes. When the event is fired inside a transaction, the publications are stored (if a registry is
 * present) as part of that transaction and the listeners are called after the commit. Storing and synchronous
 * listeners never run on an I/O thread: without a transaction, work fired from an event loop moves to the listener
 * executor.
 */
@Singleton
public class ModuleEventDispatcher implements EventRedelivery {

    private static final Logger LOG = Logger.getLogger(ModuleEventDispatcher.class);
    private static final Set<Class<? extends Annotation>> LIFECYCLE_QUALIFIERS = Set.of(Initialized.class,
            BeforeDestroyed.class, Destroyed.class);

    @Inject
    ListenerTable table;

    @Inject
    TransactionSupport transactions;

    @Inject
    Instance<EventPublicationRegistry> registries;

    @Inject
    Instance<ReadOnlyTransactions> readOnlyTransactions;

    @Inject
    ListenerExecutor executor;

    @Inject
    ListenerInvokers invokers;

    @Inject
    ListenerConfig config;

    Optional<EventPublicationRegistry> registry = Optional.empty();
    Optional<ReadOnlyTransactions> readOnly = Optional.empty();

    private final Map<String, Listener> listenersById = new LinkedHashMap<>();
    private final Map<String, List<Listener>> listenersByObservedType = new HashMap<>();
    private final InFlightDeliveries inFlight = new InFlightDeliveries();
    private final AtomicInteger abandoned = new AtomicInteger();

    /**
     * Binds the invokers and conditions to the listeners and resolves the optional collaborators. The table is fixed
     * at build time, so this happens once.
     *
     * @throws IllegalStateException if more than one bean implements a collaborator, which would otherwise turn the
     *         event publication registry off without notice
     */
    @PostConstruct
    void init() {
        bind();
        registry = optional(registries, EventPublicationRegistry.class);
        readOnly = optional(readOnlyTransactions, ReadOnlyTransactions.class);
        ModuleEventObservers.CURRENT.set(this);
    }

    static <T> Optional<T> optional(Instance<T> instance, Class<T> type) {
        if (instance.isAmbiguous()) {
            throw new IllegalStateException("More than one bean implements " + type.getName()
                    + ", the module event dispatcher needs one at most");
        }
        return instance.isResolvable() ? Optional.of(instance.get()) : Optional.empty();
    }

    @PreDestroy
    void destroy() {
        ModuleEventObservers.CURRENT.compareAndSet(this, null);
    }

    /**
     * Builds the routing maps from the listener table and the generated invokers.
     */
    void bind() {
        Map<String, Invoker<Object, Object>> allInvokers = invokers.invokers();
        Map<String, Predicate<Object>> conditions = invokers.conditions();
        for (ListenerData data : table.listeners()) {
            Listener listener = new Listener(data, allInvokers.get(data.id()),
                    Optional.ofNullable(conditions.get(data.id())));
            listenersById.put(data.id(), listener);
            listenersByObservedType.computeIfAbsent(data.observedType(), k -> new ArrayList<>()).add(listener);
        }
    }

    /**
     * Called by the synthetic observer of one listened type. CDI already resolved the type hierarchy, so the event
     * goes to the listeners of exactly that type.
     */
    public void dispatch(EventContext<Object> context, String observedType) {
        deliver(context.getEvent(), listenersByObservedType.getOrDefault(observedType, List.of()));
    }

    @Override
    public boolean redeliver(UUID publicationId, String listenerId, Object event) {
        Listener listener = listenersById.get(listenerId);
        if (Objects.isNull(listener)) {
            LOG.warnf("Cannot resubmit event publication %s: no listener with id %s", publicationId, listenerId);
            return false;
        }
        submit(new Delivery(listener, event, Optional.of(publicationId)));
        return true;
    }

    /**
     * A seam for the extension tests of this project.
     *
     * @return the identifiers of all registered module listeners
     */
    public Set<String> listenerIds() {
        return Collections.unmodifiableSet(listenersById.keySet());
    }

    /**
     * Waits until no listener is running or queued. A seam for the extension tests of this project.
     *
     * @return {@code true} if the dispatcher became idle within the timeout
     */
    public boolean awaitIdle(Duration timeout) {
        return inFlight.awaitIdle(timeout);
    }

    /**
     * Stops taking deliveries and waits for those in flight, up to {@code quarkus.modulith.listeners.shutdown-timeout}.
     * Runs before the event publication registry reports what is left.
     */
    void onShutdown(@Observes @Priority(100) ShutdownEvent event) {
        inFlight.stop();
        if (!inFlight.awaitIdle(config.shutdownTimeout())) {
            LOG.warnf("%d module listener deliveries were still running after %s at shutdown", inFlight.count(),
                    config.shutdownTimeout());
        }
        if (abandoned.get() > 0) {
            LOG.warnf("%d module listener deliveries were not started because the application is stopping",
                    abandoned.get());
        }
    }

    /**
     * A seam for unit tests.
     *
     * @return the number of deliveries refused because the application is stopping
     */
    int abandoned() {
        return abandoned.get();
    }

    /**
     * Activates the request context for a delivery unless it is active already. A seam for unit tests.
     *
     * @return whether the context was activated and must be terminated after the delivery
     */
    boolean activateRequestContext() {
        ManagedContext context = Arc.container().requestContext();
        if (context.isActive()) {
            return false;
        }
        context.activate();
        return true;
    }

    void terminateRequestContext() {
        Arc.container().requestContext().terminate();
    }

    /**
     * A seam for unit tests.
     */
    boolean isBlockingAllowed() {
        return BlockingOperationControl.isBlockingAllowed();
    }

    static boolean isLifecycleEvent(EventContext<Object> context) {
        for (Annotation qualifier : context.getMetadata().getQualifiers()) {
            if (LIFECYCLE_QUALIFIERS.contains(qualifier.annotationType())) {
                return true;
            }
        }
        return false;
    }

    private void deliver(Object event, List<Listener> candidates) {
        if (candidates.isEmpty()) {
            return;
        }
        List<Listener> targets = applicable(event, candidates);
        if (targets.isEmpty()) {
            return;
        }
        if (!isBlockingAllowed() && !transactions.isActive()) {
            // Fired from an I/O thread: storing and synchronous listeners would block it
            inFlight.begin();
            try {
                executor.execute(() -> {
                    try {
                        storeAndSubmit(event, targets);
                    } finally {
                        inFlight.end();
                    }
                });
            } catch (RuntimeException e) {
                inFlight.end();
                // Nothing was stored yet, and blocking the I/O thread to deliver synchronously is not an option
                LOG.errorf(e, "Could not hand an event of type %s to the module listener executor, the event is lost"
                        + " for module listeners %s", event.getClass().getName(), ids(targets));
            }
            return;
        }
        storeAndSubmit(event, targets);
    }

    private List<Listener> applicable(Object event, List<Listener> candidates) {
        List<Listener> targets = candidates;
        for (int i = 0; i < candidates.size(); i++) {
            if (!applies(candidates.get(i), event)) {
                targets = new ArrayList<>(candidates.size());
                for (Listener listener : candidates) {
                    if (applies(listener, event)) {
                        targets.add(listener);
                    }
                }
                break;
            }
        }
        return targets;
    }

    /**
     * The compiled condition covers both the {@code condition} attribute and listener beans implementing
     * {@link io.quarkiverse.modulith.events.ConditionalEventListener}.
     */
    private static boolean applies(Listener listener, Object event) {
        return listener.condition().map(condition -> condition.test(event)).orElse(true);
    }

    private void storeAndSubmit(Object event, List<Listener> targets) {
        List<Optional<UUID>> publicationIds = store(event, targets);
        List<Delivery> deliveries = new ArrayList<>(targets.size());
        for (int i = 0; i < targets.size(); i++) {
            deliveries.add(new Delivery(targets.get(i), event, publicationIds.get(i)));
        }
        if (transactions.isActive()) {
            Runnable action = () -> deliveries.forEach(this::submit);
            registry.ifPresentOrElse(store -> store.afterCommit(action, transactions::afterCommit),
                    () -> transactions.afterCommit(action));
        } else {
            deliveries.forEach(this::submit);
        }
    }

    private List<Optional<UUID>> store(Object event, List<Listener> targets) {
        if (registry.isEmpty()) {
            return Collections.nCopies(targets.size(), Optional.empty());
        }
        return registry.get().store(event, ids(targets)).stream().map(Optional::of).toList();
    }

    private static List<String> ids(List<Listener> listeners) {
        return listeners.stream().map(listener -> listener.data().id()).toList();
    }

    private void submit(Delivery delivery) {
        if (!inFlight.tryBegin()) {
            abandoned.incrementAndGet();
            return;
        }
        if (delivery.listener().data().async()) {
            try {
                executor.execute(() -> invoke(delivery));
            } catch (RuntimeException e) {
                inFlight.end();
                LOG.errorf(e, "Could not schedule module listener %s (publication %s stays incomplete)",
                        delivery.listener().data().id(), publication(delivery));
            }
        } else {
            invoke(delivery);
        }
    }

    /**
     * An {@link Error} is not caught: it propagates once the request context and the in-flight count are released.
     */
    private void invoke(Delivery delivery) {
        boolean activated = false;
        try {
            activated = activateRequestContext();
            Optional<UUID> publicationId = delivery.publicationId();
            if (publicationId.isPresent() && !claim(publicationId.get())) {
                LOG.debugf("Skipping module listener %s: publication %s is completed or gone",
                        delivery.listener().data().id(), publicationId.get());
                return;
            }
            if (!callInTransaction(delivery)) {
                publicationId.ifPresent(id -> completeAfterListener(delivery, id));
            }
        } catch (Exception e) {
            failed(delivery, e instanceof ListenerFailure ? e.getCause() : e);
        } finally {
            if (activated) {
                terminateRequestContext();
            }
            inFlight.end();
        }
    }

    /**
     * Calls the listener with the transaction semantics it declares, and marks the publication completed in that
     * transaction when the completion timing asks for it.
     *
     * @return whether the publication was marked completed
     */
    private boolean callInTransaction(Delivery delivery) {
        ListenerData data = delivery.listener().data();
        boolean completeInListener = config.completionTiming() == CompletionTiming.IN_LISTENER;
        AtomicBoolean completed = new AtomicBoolean();
        transactions.run(data.propagation(), () -> {
            if (data.readOnly()) {
                readOnly.ifPresent(ReadOnlyTransactions::markReadOnly);
            }
            call(delivery);
            if (completeInListener && transactions.isActive()) {
                delivery.publicationId().ifPresent(id -> {
                    registry().markCompleted(id);
                    completed.set(true);
                });
            }
        });
        return completed.get();
    }

    /**
     * The listener succeeded, so a failure here does not mark the publication failed: it stays incomplete and is
     * submitted again like any incomplete publication.
     */
    private void completeAfterListener(Delivery delivery, UUID publicationId) {
        try {
            transactions.inNewTransaction(() -> registry().markCompleted(publicationId));
        } catch (RuntimeException e) {
            LOG.errorf(e, "Module listener %s succeeded, but event publication %s could not be marked completed and"
                    + " stays incomplete", delivery.listener().data().id(), publicationId);
        }
    }

    /**
     * A serialization failure means another instance claimed the publication concurrently, so this one does not run
     * it and does not mark it failed.
     */
    private boolean claim(UUID publicationId) {
        try {
            return transactions.inNewTransaction(() -> registry().markProcessing(publicationId));
        } catch (RuntimeException e) {
            if (SerializationFailures.isSerializationFailure(e)) {
                return false;
            }
            throw e;
        }
    }

    private static void call(Delivery delivery) {
        try {
            delivery.listener().invoker().invoke(null, new Object[] { delivery.event() });
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ListenerFailure(e);
        }
    }

    private void failed(Delivery delivery, Throwable cause) {
        ListenerData data = delivery.listener().data();
        // The event itself is logged at DEBUG only, it may carry personal data
        LOG.errorf(cause, "Module listener %s failed for an event of type %s (publication %s)", data.id(),
                delivery.event().getClass().getName(), publication(delivery));
        LOG.debugf("Event that module listener %s failed for: %s", data.id(), delivery.event());
        delivery.publicationId().ifPresent(id -> {
            try {
                transactions.inNewTransaction(() -> registry().markFailed(id, cause));
            } catch (RuntimeException e) {
                LOG.errorf(e, "Could not mark event publication %s as failed", id);
            }
        });
    }

    /**
     * Only deliveries with a publication identifier use the registry, and those exist only when there is a registry.
     */
    private EventPublicationRegistry registry() {
        return registry.orElseThrow(() -> new IllegalStateException(
                "An event publication without an event publication registry"));
    }

    private static String publication(Delivery delivery) {
        return delivery.publicationId().map(UUID::toString).orElse("none");
    }
}
