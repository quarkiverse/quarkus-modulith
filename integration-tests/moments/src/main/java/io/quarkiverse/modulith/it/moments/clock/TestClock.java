package io.quarkiverse.modulith.it.moments.clock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Singleton;

/**
 * The application's {@link Clock}. Moments picks it up instead of the system clock. It stands still until it is set
 * through {@link TimeResource}, so the tests never depend on the real time. The copies {@link #withZone(ZoneId)}
 * returns read the same instant.
 */
@Singleton
public class TestClock extends Clock {

    private final AtomicReference<Instant> instant;
    private final ZoneId zone;

    public TestClock() {
        this(new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z")), ZoneOffset.UTC);
    }

    private TestClock(AtomicReference<Instant> instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    void set(Instant instant) {
        this.instant.set(instant);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new TestClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant.get();
    }
}
