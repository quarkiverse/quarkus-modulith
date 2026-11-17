package io.quarkiverse.modulith.events.externalization.runtime;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.ResubmissionExclusion;

/**
 * Keeps the event publication registry from resubmitting outbox publications: only the {@link OutboxDrain} sends
 * them, so that they reach the broker in publication order.
 */
@Singleton
public class OutboxResubmissionExclusion implements ResubmissionExclusion {

    @Override
    public boolean excludes(PublicationTargetIdentifier listener) {
        return OutboxEventExternalizer.LISTENER_ID.equals(listener.value());
    }
}
