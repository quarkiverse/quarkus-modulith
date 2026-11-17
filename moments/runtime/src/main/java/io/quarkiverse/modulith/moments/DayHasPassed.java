package io.quarkiverse.modulith.moments;

import java.time.LocalDate;
import java.util.Objects;

import org.jmolecules.event.types.DomainEvent;

/**
 * A {@link DomainEvent} published at the end of every day.
 */
public class DayHasPassed implements DomainEvent {

    /**
     * The day that has just passed.
     */
    private final LocalDate date;

    private DayHasPassed(LocalDate date) {
        this.date = Objects.requireNonNull(date, "LocalDate must not be null!");
    }

    /**
     * Creates a new {@link DayHasPassed} for the given {@link LocalDate}.
     *
     * @param date the day that has just passed, must not be {@literal null}
     * @return the event
     */
    public static DayHasPassed of(LocalDate date) {
        return new DayHasPassed(date);
    }

    /**
     * The day that has just passed.
     *
     * @return never {@literal null}
     */
    public LocalDate getDate() {
        return date;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DayHasPassed that)) {
            return false;
        }
        return Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date);
    }

    @Override
    public String toString() {
        return "DayHasPassed[" + date + "]";
    }
}
