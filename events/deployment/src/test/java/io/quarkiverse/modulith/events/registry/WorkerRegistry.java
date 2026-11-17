package io.quarkiverse.modulith.events.registry;

import java.time.Clock;

import io.quarkiverse.modulith.events.config.EventsConfig;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.spi.EventRedelivery;
import io.quarkiverse.modulith.events.spi.TransactionRunner;

/**
 * The registry built with its public constructor, running where blocking is allowed, outside Quarkus.
 */
final class WorkerRegistry extends DefaultEventPublicationRegistry {

    WorkerRegistry(EventPublicationRepository repository, TransactionRunner transactions, EventsConfig config,
            Clock clock, EventRedelivery redelivery) {
        super(repository, transactions, config, clock, redelivery);
    }

    @Override
    boolean isBlockingAllowed() {
        return true;
    }
}
