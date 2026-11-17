package io.quarkiverse.modulith.events.externalization.runtime;

import java.util.UUID;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.events.externalization.ExternalizationStrategy;
import io.quarkiverse.modulith.events.spi.EventRedelivery;

/**
 * Routes the resubmission of externalization publications to the externalization strategy, and every other
 * resubmission to the module event dispatcher it decorates. This way the event publication registry resubmits
 * externalized events through the same API as module listener events.
 */
@Decorator
@Priority(10)
public class ExternalizationRedeliveryDecorator implements EventRedelivery {

    @Inject
    @Delegate
    EventRedelivery delegate;

    /**
     * Resolved on use: the decorated dispatcher may be created during static initialization, before the runtime
     * configuration the strategy needs exists.
     */
    @Inject
    Instance<ExternalizationStrategy> strategies;

    @Override
    public boolean redeliver(UUID publicationId, String listenerId, Object event) {
        ExternalizationStrategy strategy = strategies.get();
        if (strategy.handles(listenerId)) {
            strategy.resubmit(publicationId, event);
            return true;
        }
        return delegate.redeliver(publicationId, listenerId, event);
    }
}
