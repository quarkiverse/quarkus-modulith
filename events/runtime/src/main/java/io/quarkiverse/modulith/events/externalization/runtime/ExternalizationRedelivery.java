package io.quarkiverse.modulith.events.externalization.runtime;

import java.util.UUID;

import io.quarkiverse.modulith.events.externalization.ExternalizationStrategy;
import io.quarkiverse.modulith.events.spi.EventRedelivery;

/**
 * Resubmits the event publications of externalization when the application has no module listeners, and so no other
 * {@link EventRedelivery}. Registered by the build only then.
 */
public class ExternalizationRedelivery implements EventRedelivery {

    private final ExternalizationStrategy strategy;

    public ExternalizationRedelivery(ExternalizationStrategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public boolean redeliver(UUID publicationId, String listenerId, Object event) {
        if (!strategy.handles(listenerId)) {
            return false;
        }
        strategy.resubmit(publicationId, event);
        return true;
    }
}
