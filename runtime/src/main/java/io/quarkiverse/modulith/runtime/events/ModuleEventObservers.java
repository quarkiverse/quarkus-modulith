package io.quarkiverse.modulith.runtime.events;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.inject.spi.EventContext;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;

/**
 * Entry points of the synthetic observers the deployment module registers, one per event type a module listener
 * listens to.
 * <p>
 * Context lifecycle events ({@code @Initialized}, {@code @BeforeDestroyed}, {@code @Destroyed}) are dropped here. Their
 * payload is a {@code String} or an {@code Object}, so a listener of such a type would otherwise receive them, and
 * activating the request context for that listener would fire the next one, recursively. Dropping them before the
 * dispatcher is looked up also keeps static-init lifecycle events from creating the dispatcher before the runtime
 * configuration exists.
 */
public final class ModuleEventObservers {

    private ModuleEventObservers() {
    }

    /**
     * @param observedType the observed type, as recorded in {@link ListenerData#observedType()}
     */
    public static void notify(EventContext<Object> context, String observedType) {
        if (!ModuleEventDispatcher.isLifecycleEvent(context)) {
            dispatcher().ifPresent(dispatcher -> dispatcher.dispatch(context, observedType));
        }
    }

    /**
     * The running dispatcher, set when it is created and cleared when it is destroyed, so that an event costs no
     * container lookup. The first event of an application creates it through the container.
     * <p>
     * A deliberate exception to the rule against static state filled at startup: the synthetic observers are
     * generated static calls, and this saves a container lookup per event. Only the dispatcher sets and clears it,
     * and it holds no build-time data. Quarkus runs one application per class loader, so a dev mode restart replaces
     * the value instead of sharing it.
     */
    static final AtomicReference<ModuleEventDispatcher> CURRENT = new AtomicReference<>();

    /**
     * @return the dispatcher, empty for an event fired after the container shut down
     */
    static Optional<ModuleEventDispatcher> dispatcher() {
        ModuleEventDispatcher current = CURRENT.get();
        if (Objects.nonNull(current)) {
            return Optional.of(current);
        }
        ArcContainer container = Arc.container();
        return Objects.isNull(container) ? Optional.empty()
                : Optional.of(container.instance(ModuleEventDispatcher.class).get());
    }
}
