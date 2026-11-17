package io.quarkiverse.modulith.moments;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.Objects;

import org.jmolecules.event.types.DomainEvent;

/**
 * A {@link DomainEvent} published at the end of December 31st of every year.
 */
public class YearHasPassed implements DomainEvent {

    /**
     * The year that has just passed.
     */
    private final Year year;

    private YearHasPassed(Year year) {
        this.year = Objects.requireNonNull(year, "Year must not be null!");
    }

    /**
     * Creates a new {@link YearHasPassed} for the given {@link Year}.
     *
     * @param year the year that has just passed, must not be {@literal null}
     * @return the event
     */
    public static YearHasPassed of(Year year) {
        return new YearHasPassed(year);
    }

    /**
     * Creates a new {@link YearHasPassed} for the given year.
     *
     * @param year the year that has just passed
     * @return the event
     */
    public static YearHasPassed of(int year) {
        return of(Year.of(year));
    }

    /**
     * The year that has just passed.
     *
     * @return never {@literal null}
     */
    public Year getYear() {
        return year;
    }

    LocalDate getStartDate() {
        return LocalDate.of(year.getValue(), Month.JANUARY, 1);
    }

    LocalDate getEndDate() {
        return LocalDate.of(year.getValue(), Month.DECEMBER, 31);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof YearHasPassed that)) {
            return false;
        }
        return Objects.equals(year, that.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(year);
    }

    @Override
    public String toString() {
        return "YearHasPassed[" + year + "]";
    }
}
