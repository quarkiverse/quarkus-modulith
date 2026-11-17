package io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.support;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.events.jobrunr.JobRunrExternalizationTransport;

@Singleton
public class DuplicateTransports {

    @Produces
    @Singleton
    @Named("first")
    JobRunrExternalizationTransport first() {
        return event -> {
        };
    }

    @Produces
    @Singleton
    @Named("second")
    JobRunrExternalizationTransport second() {
        return event -> {
        };
    }
}
