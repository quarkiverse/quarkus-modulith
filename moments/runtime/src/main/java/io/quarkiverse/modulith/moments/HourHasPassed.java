package io.quarkiverse.modulith.moments;

import java.time.LocalDateTime;
import java.util.Objects;

import org.jmolecules.event.types.DomainEvent;

/**
 * A {@link DomainEvent} published at the end of every hour.
 */
public class HourHasPassed implements DomainEvent {

    /**
     * The hour that has just passed.
     */
    private final LocalDateTime time;

    private HourHasPassed(LocalDateTime time) {
        this.time = Objects.requireNonNull(time, "LocalDateTime must not be null!");
    }

    /**
     * Creates a new {@link HourHasPassed} for the given {@link LocalDateTime}.
     *
     * @param time the start of the hour that has just passed, must not be {@literal null}
     * @return the event
     */
    public static HourHasPassed of(LocalDateTime time) {
        return new HourHasPassed(time);
    }

    /**
     * The hour that has just passed.
     *
     * @return the start of the hour, never {@literal null}
     */
    public LocalDateTime getTime() {
        return time;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HourHasPassed that)) {
            return false;
        }
        return Objects.equals(time, that.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time);
    }

    @Override
    public String toString() {
        return "HourHasPassed[" + time + "]";
    }
}
