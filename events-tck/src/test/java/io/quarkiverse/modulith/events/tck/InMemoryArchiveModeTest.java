package io.quarkiverse.modulith.events.tck;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;

class InMemoryArchiveModeTest extends ArchiveCompletionModeContract {

    private final InMemoryEventPublicationRepository repository = new InMemoryEventPublicationRepository(
            CompletionMode.ARCHIVE);

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }
}
