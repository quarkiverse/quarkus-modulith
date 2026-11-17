package io.quarkiverse.modulith.it.externalizationrabbitmq.registry;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;

public class Repositories {

    @Produces
    @Singleton
    EventPublicationRepository repository() {
        return new InMemoryEventPublicationRepository();
    }
}
