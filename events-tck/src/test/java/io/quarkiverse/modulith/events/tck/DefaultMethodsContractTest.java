package io.quarkiverse.modulith.events.tck;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;

class DefaultMethodsContractTest extends EventPublicationRepositoryContract {

    private final AbstractMethodsOnlyRepository repository = new AbstractMethodsOnlyRepository();

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }
}
