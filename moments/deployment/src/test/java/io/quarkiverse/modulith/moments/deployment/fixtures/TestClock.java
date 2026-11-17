package io.quarkiverse.modulith.moments.deployment.fixtures;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import jakarta.inject.Singleton;

/**
 * The application's {@link Clock} bean in tests. It stands still at 2026-03-31T21:58:30Z until a test moves it.
 */
@Singleton
public class TestClock extends Clock {

    public static final Instant START = Instant.parse("2026-03-31T21:58:30Z");

    private volatile Instant instant = START;

    public void set(Instant instant) {
        this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
