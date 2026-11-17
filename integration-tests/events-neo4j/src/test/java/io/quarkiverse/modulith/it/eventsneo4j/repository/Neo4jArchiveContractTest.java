package io.quarkiverse.modulith.it.eventsneo4j.repository;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.tck.ArchiveCompletionModeContract;

class Neo4jArchiveContractTest extends ArchiveCompletionModeContract {

    private final EventPublicationRepository repository = Neo4j.repository(Neo4j.emptyServer(),
            CompletionMode.ARCHIVE);

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }
}
