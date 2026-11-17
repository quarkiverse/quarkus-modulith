package io.quarkiverse.modulith.events.tck;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;

class InMemoryDeleteModeTest extends DeleteCompletionModeContract {

    private final InMemoryEventPublicationRepository repository = new InMemoryEventPublicationRepository(
            CompletionMode.DELETE);

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }
}
