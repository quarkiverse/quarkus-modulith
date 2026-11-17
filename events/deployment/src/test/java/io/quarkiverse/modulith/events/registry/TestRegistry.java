package io.quarkiverse.modulith.events.registry;

import java.time.Clock;
import java.util.Optional;

import io.quarkiverse.modulith.events.config.EventsConfig;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.spi.EventRedelivery;

/**
 * The registry wired by hand, with the redelivery target under the test's control.
 */
final class TestRegistry extends DefaultEventPublicationRegistry {

    private final Optional<EventRedelivery> target;

    TestRegistry(EventPublicationRepository repository, EventsConfig config, Clock clock,
            Optional<EventRedelivery> target) {
        this.repository = repository;
        this.config = config;
        this.clock = clock;
        this.transactions = new DirectTransactions();
        this.target = target;
    }

    @Override
    Optional<EventRedelivery> redelivery() {
        return target;
    }

    @Override
    boolean isBlockingAllowed() {
        return true;
    }
}
