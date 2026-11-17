package io.quarkiverse.modulith.events.externalization.runtime;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import io.quarkiverse.modulith.events.core.EventPublicationRepository;
import io.quarkiverse.modulith.events.externalization.fakes.FixedInstance;

final class TestPublications {

    static final Instant NOW = Instant.parse("2026-11-16T10:00:00Z");

    private TestPublications() {
    }

    static <T extends ExternalizationPublications> T initialize(T publications,
            Optional<EventPublicationRepository> repository) {
        return initialize(publications, repository, NOW);
    }

    static <T extends ExternalizationPublications> T initialize(T publications,
            Optional<EventPublicationRepository> repository, Instant now) {
        publications.repositories = new FixedInstance<>(repository);
        publications.clocks = new FixedInstance<>(Optional.of(Clock.fixed(now, ZoneOffset.UTC)));
        publications.init();
        return publications;
    }
}
