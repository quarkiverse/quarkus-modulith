package io.quarkiverse.modulith.observability.deployment;

import java.time.Instant;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.core.PublicationTargetIdentifier;
import io.quarkiverse.modulith.events.core.TargetEventPublication;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;

/**
 * An event publication repository with one published publication.
 */
public class TestRepository {

    @Produces
    @Singleton
    EventPublicationRepository repository() {
        InMemoryEventPublicationRepository repository = new InMemoryEventPublicationRepository();
        repository.create(TargetEventPublication.of("event", PublicationTargetIdentifier.of("listener"), Instant.now()));
        return repository;
    }
}
