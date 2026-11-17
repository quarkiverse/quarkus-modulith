package io.quarkiverse.modulith.events.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.quarkiverse.modulith.events.config.EventsConfig;

/**
 * The jobs wired by hand, running background work inline.
 */
final class TestJobs extends EventPublicationJobs {

    final List<String> background = new ArrayList<>();
    private final Optional<Boolean> legacy;
    private final Optional<PeriodicJobs> scheduler;

    TestJobs(DefaultEventPublicationRegistry registry, EventsConfig config, Optional<Boolean> legacy,
            Optional<PeriodicJobs> scheduler) {
        this.registry = registry;
        this.config = config;
        this.legacy = legacy;
        this.scheduler = scheduler;
    }

    @Override
    Optional<Boolean> legacyRepublishOnRestart() {
        return legacy;
    }

    @Override
    Optional<PeriodicJobs> periodicJobs() {
        return scheduler;
    }

    @Override
    void runInBackground(Runnable task) {
        background.add("ran");
        task.run();
    }
}
