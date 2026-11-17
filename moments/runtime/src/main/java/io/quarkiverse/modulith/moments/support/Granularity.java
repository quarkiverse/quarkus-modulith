package io.quarkiverse.modulith.moments.support;

import java.time.temporal.ChronoUnit;

/**
 * The finest unit of passage of time events to publish. Each granularity includes the coarser ones: {@link #HOURS}
 * publishes hourly and daily events (and the week, month, quarter and year events derived from the days),
 * {@link #DAYS} only the daily ones.
 */
public enum Granularity {

    /**
     * Publish an event every second, minute, hour and day. Added by Spring Modulith 2.2.
     */
    SECONDS(ChronoUnit.SECONDS),

    /**
     * Publish an event every minute, hour and day. Added by Spring Modulith 2.2.
     */
    MINUTES(ChronoUnit.MINUTES),

    /**
     * Publish hourly and daily events.
     */
    HOURS(ChronoUnit.HOURS),

    /**
     * Publish daily events only.
     */
    DAYS(ChronoUnit.DAYS);

    private final ChronoUnit unit;

    Granularity(ChronoUnit unit) {
        this.unit = unit;
    }

    /**
     * The time unit of the finest events.
     *
     * @return never {@literal null}
     */
    public ChronoUnit getUnit() {
        return unit;
    }

    /**
     * Whether events of the given granularity are published when this one is configured.
     *
     * @param other must not be {@literal null}
     * @return {@code true} if {@code other} is this granularity or a coarser one
     */
    public boolean includes(Granularity other) {
        return compareTo(other) <= 0;
    }
}
