package io.quarkiverse.modulith.events.externalization.runtime;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.EventExternalizationConfiguration;
import io.quarkiverse.modulith.events.externalization.ExternalizationStrategy;
import io.quarkiverse.modulith.events.spi.ListenerExecutor;
import io.quarkus.runtime.BlockingOperationControl;

/**
 * The outbox mode on the event publication registry: the publication of each externalized event is recorded in the
 * publishing transaction and is the outbox row. The {@link OutboxDrain} sends the rows in publication order after the
 * commit and on a schedule, so that events recorded while the broker was down are sent once it is back.
 */
@Singleton
public class OutboxEventExternalizer implements ExternalizationStrategy {

    /**
     * The listener identifier stored with the outbox publications.
     */
    public static final String LISTENER_ID = "io.quarkiverse.modulith.events.externalization.runtime."
            + "OutboxEventExternalizer.externalize(java.lang.Object)";

    @Inject
    EventExternalizationConfiguration configuration;

    @Inject
    ExternalizationPublications publications;

    @Inject
    ExternalizationTransactions transactions;

    @Inject
    OutboxDrain drain;

    @Inject
    ListenerExecutor executor;

    @Override
    public void onEvent(Object event) {
        if (!configuration.supports(event)) {
            return;
        }
        if (transactions.isActive() || BlockingOperationControl.isBlockingAllowed()) {
            publications.store(event, LISTENER_ID);
            transactions.afterCommit(drain::trigger);
        } else {
            executor.execute(() -> {
                publications.store(event, LISTENER_ID);
                drain.trigger();
            });
        }
    }

    @Override
    public boolean handles(String listenerId) {
        return LISTENER_ID.equals(listenerId);
    }

    /**
     * The registry skips outbox publications (see {@link OutboxResubmissionExclusion}). An explicit resubmission
     * runs the drain, which sends the publication in order with the others.
     */
    @Override
    public void resubmit(UUID publicationId, Object event) {
        drain.trigger();
    }
}
