package io.quarkiverse.modulith.it.eventsneo4j.repository;

import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.tck.EventPublicationRepositoryContract;

@ParameterizedClass(name = "{0}")
@EnumSource(value = CompletionMode.class, names = { "UPDATE", "ARCHIVE" })
class Neo4jRepositoryContractTest extends EventPublicationRepositoryContract {

    private final EventPublicationRepository repository;

    Neo4jRepositoryContractTest(CompletionMode completion) {
        this.repository = Neo4j.repository(Neo4j.emptyServer(), completion);
    }

    @Override
    protected EventPublicationRepository repository() {
        return repository;
    }
}
