package io.quarkiverse.modulith.events.tck;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;

class InMemoryEventPublicationRepositoryTest extends EventPublicationRepositoryContract {

    private final InMemoryEventPublicationRepository repository = new InMemoryEventPublicationRepository();

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }
}
