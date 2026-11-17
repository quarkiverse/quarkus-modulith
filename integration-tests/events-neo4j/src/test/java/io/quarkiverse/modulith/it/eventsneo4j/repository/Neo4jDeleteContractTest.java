package io.quarkiverse.modulith.it.eventsneo4j.repository;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.tck.DeleteCompletionModeContract;

class Neo4jDeleteContractTest extends DeleteCompletionModeContract {

    private final EventPublicationRepository repository = Neo4j.repository(Neo4j.emptyServer(),
            CompletionMode.DELETE);

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }
}
