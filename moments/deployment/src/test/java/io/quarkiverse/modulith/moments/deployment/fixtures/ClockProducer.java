package io.quarkiverse.modulith.moments.deployment.fixtures;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Declares a fixed {@link Clock} bean. Its zone is ignored by Moments, which uses the configured zone.
 */
@Singleton
public class ClockProducer {

    public static final ZoneId ZONE = ZoneOffset.ofHours(9);

    @Produces
    @Singleton
    Clock clock() {
        return Clock.fixed(Instant.parse("2026-11-16T10:15:30Z"), ZONE);
    }
}
