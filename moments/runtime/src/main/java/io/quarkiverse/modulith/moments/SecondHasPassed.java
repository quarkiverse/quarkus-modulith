package io.quarkiverse.modulith.moments;

import java.time.LocalDateTime;
import java.util.Objects;

import org.jmolecules.event.types.DomainEvent;

/**
 * A {@link DomainEvent} published at the end of every second when the granularity is {@code seconds}. Spring Modulith
 * 2.2 adds the same type.
 */
public class SecondHasPassed implements DomainEvent {

    /**
     * The second that has just passed.
     */
    private final LocalDateTime time;

    private SecondHasPassed(LocalDateTime time) {
        this.time = Objects.requireNonNull(time, "LocalDateTime must not be null!");
    }

    /**
     * Creates a new {@link SecondHasPassed} for the given {@link LocalDateTime}.
     *
     * @param time the start of the second that has just passed, must not be {@literal null}
     * @return the event
     */
    public static SecondHasPassed of(LocalDateTime time) {
        return new SecondHasPassed(time);
    }

    /**
     * The second that has just passed.
     *
     * @return the start of the second, never {@literal null}
     */
    public LocalDateTime getTime() {
        return time;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SecondHasPassed that)) {
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
        return "SecondHasPassed[" + time + "]";
    }
}
