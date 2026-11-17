package io.quarkiverse.modulith.events.registry;

import java.time.Duration;
import java.util.Optional;

import io.quarkiverse.modulith.events.config.EventsConfig;

record FakeStaleness(Optional<Duration> published, Optional<Duration> processing, Optional<Duration> resubmitted,
        Duration checkInterval) implements EventsConfig.Staleness {

    static FakeStaleness none() {
        return new FakeStaleness(Optional.empty(), Optional.empty(), Optional.empty(), Duration.ofMinutes(1));
    }
}
