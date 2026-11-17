package io.quarkiverse.modulith.it.eventsjdbc.repository;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;

public record RepositoryTarget(TestDatabase database, boolean legacy, CompletionMode completion) {

    @Override
    public String toString() {
        return database + (legacy ? " v1 " : " v2 ") + completion;
    }
}
