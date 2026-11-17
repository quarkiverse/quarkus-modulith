package io.quarkiverse.modulith.events.jpa.deployment.readonly;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

public class FixedClock {

    public static final Instant NOW = Instant.parse("2030-05-01T08:00:00Z");

    @Produces
    @Singleton
    Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
