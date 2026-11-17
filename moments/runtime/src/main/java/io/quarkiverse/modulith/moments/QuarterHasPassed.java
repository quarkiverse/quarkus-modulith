package io.quarkiverse.modulith.moments;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.Objects;

import org.jmolecules.event.types.DomainEvent;

/**
 * A {@link DomainEvent} published at the end of the last day of every quarter. The year is the calendar year the
 * fiscal year of the {@link ShiftedQuarter} starts in.
 */
public class QuarterHasPassed implements DomainEvent {

    private final Year year;
    private final ShiftedQuarter quarter;

    private QuarterHasPassed(Year year, ShiftedQuarter quarter) {
        this.year = Objects.requireNonNull(year, "Year must not be null!");
        this.quarter = Objects.requireNonNull(quarter, "ShiftedQuarter must not be null!");
    }

    /**
     * Creates a new {@link QuarterHasPassed} for the given {@link Year} and {@link ShiftedQuarter}.
     *
     * @param year the calendar year the fiscal year starts in, must not be {@literal null}
     * @param quarter must not be {@literal null}
     * @return the event
     */
    public static QuarterHasPassed of(Year year, ShiftedQuarter quarter) {
        return new QuarterHasPassed(year, quarter);
    }

    /**
     * Creates a new {@link QuarterHasPassed} for the given {@link Year} and calendar {@link Quarter}.
     *
     * @param year must not be {@literal null}
     * @param quarter must not be {@literal null}
     * @return the event
     */
    public static QuarterHasPassed of(Year year, Quarter quarter) {
        return of(year, ShiftedQuarter.of(quarter));
    }

    /**
     * Creates a new {@link QuarterHasPassed} for the given {@link Year} and {@link Quarter} of a fiscal year starting
     * in the given month.
     *
     * @param year the calendar year the fiscal year starts in, must not be {@literal null}
     * @param quarter must not be {@literal null}
     * @param startMonth the first month of the fiscal year, must not be {@literal null}
     * @return the event
     */
    public static QuarterHasPassed of(Year year, Quarter quarter, Month startMonth) {
        return of(year, ShiftedQuarter.of(quarter, startMonth));
    }

    /**
     * The calendar year the fiscal year of the quarter starts in.
     *
     * @return never {@literal null}
     */
    public Year getYear() {
        return year;
    }

    /**
     * The quarter that has just passed.
     *
     * @return never {@literal null}
     */
    public ShiftedQuarter getQuarter() {
        return quarter;
    }

    /**
     * The first day of the quarter.
     *
     * @return never {@literal null}
     */
    public LocalDate getStartDate() {
        return quarter.getStartDate(year);
    }

    /**
     * The last day of the quarter.
     *
     * @return never {@literal null}
     */
    public LocalDate getEndDate() {
        return quarter.getEndDate(year);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof QuarterHasPassed that)) {
            return false;
        }
        return Objects.equals(quarter, that.quarter) && Objects.equals(year, that.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quarter, year);
    }

    @Override
    public String toString() {
        return "QuarterHasPassed[" + year + " " + quarter + "]";
    }
}
