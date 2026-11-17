package io.quarkiverse.modulith.it.externalizationjms.broker;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;

/**
 * Keeps the event publications in memory, standing in for an event publication store.
 */
public class Repositories {

    @Produces
    @Singleton
    EventPublicationRepository repository() {
        return new InMemoryEventPublicationRepository();
    }
}
