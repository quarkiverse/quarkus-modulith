package io.quarkiverse.modulith.events.tck;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;

class WithoutStatusContractTest extends EventPublicationRepositoryContract {

    private final StatuslessRepository repository = new StatuslessRepository();

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }

    @Override
    protected boolean tracksStatus() {
        return false;
    }
}
