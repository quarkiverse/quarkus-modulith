package io.quarkiverse.modulith.events.jpa.deployment.replaced;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.spi.ReadOnlyTransactions;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;

/**
 * An application that brings its own store and read-only transactions, for example to decorate them in a test.
 */
@Singleton
public class OwnStore {

    public static final ReadOnlyTransactions NO_READ_ONLY = () -> {
    };

    @Produces
    @Singleton
    EventPublicationRepository repository() {
        return new InMemoryEventPublicationRepository();
    }

    @Produces
    @Singleton
    ReadOnlyTransactions readOnlyTransactions() {
        return NO_READ_ONLY;
    }
}
