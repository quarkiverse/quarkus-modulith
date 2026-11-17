package io.quarkiverse.modulith.moments;

import java.time.YearMonth;
import java.util.Objects;

import org.jmolecules.event.types.DomainEvent;

/**
 * A {@link DomainEvent} published at the end of the last day of every month.
 */
public class MonthHasPassed implements DomainEvent {

    /**
     * The month that has just passed.
     */
    private final YearMonth month;

    private MonthHasPassed(YearMonth month) {
        this.month = Objects.requireNonNull(month, "YearMonth must not be null!");
    }

    /**
     * Creates a new {@link MonthHasPassed} for the given {@link YearMonth}.
     *
     * @param month the month that has just passed, must not be {@literal null}
     * @return the event
     */
    public static MonthHasPassed of(YearMonth month) {
        return new MonthHasPassed(month);
    }

    /**
     * The month that has just passed.
     *
     * @return never {@literal null}
     */
    public YearMonth getMonth() {
        return month;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MonthHasPassed that)) {
            return false;
        }
        return Objects.equals(month, that.month);
    }

    @Override
    public int hashCode() {
        return Objects.hash(month);
    }

    @Override
    public String toString() {
        return "MonthHasPassed[" + month + "]";
    }
}
