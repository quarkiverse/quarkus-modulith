package io.quarkiverse.modulith.moments;

import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.Year;
import java.util.Objects;

/**
 * A {@link Quarter} of a fiscal year that starts in a given month. With {@link Month#APRIL} as start month,
 * {@link Quarter#Q1} runs from April 1st to June 30th and {@link Quarter#Q4} from January 1st to March 31st of the
 * following calendar year.
 * <p>
 * A fiscal year is identified by the calendar year it starts in, so {@link #getStartDate(Year)} and
 * {@link #getEndDate(Year)} of {@link Quarter#Q4} with start month {@link Month#APRIL} and year 2026 return January 1st
 * and March 31st 2027.
 */
public class ShiftedQuarter {

    private static final MonthDay FIRST_DAY = MonthDay.of(Month.JANUARY, 1);
    private static final MonthDay LAST_DAY = MonthDay.of(Month.DECEMBER, 31);

    private final Quarter quarter;
    private final Month startMonth;

    private ShiftedQuarter(Quarter quarter, Month startMonth) {
        this.quarter = Objects.requireNonNull(quarter, "Quarter must not be null!");
        this.startMonth = Objects.requireNonNull(startMonth, "Start Month must not be null!");
    }

    /**
     * Creates a new {@link ShiftedQuarter} for the given {@link Quarter} of a fiscal year starting in the given month.
     *
     * @param quarter must not be {@literal null}
     * @param startMonth the first month of the fiscal year, must not be {@literal null}
     * @return the shifted quarter
     */
    public static ShiftedQuarter of(Quarter quarter, Month startMonth) {
        return new ShiftedQuarter(quarter, startMonth);
    }

    /**
     * Creates a new {@link ShiftedQuarter} for the given {@link Quarter} of a fiscal year starting in January, so the
     * calendar quarter itself.
     *
     * @param quarter must not be {@literal null}
     * @return the shifted quarter
     */
    public static ShiftedQuarter of(Quarter quarter) {
        return of(quarter, Month.JANUARY);
    }

    /**
     * Returns the logical {@link Quarter}.
     *
     * @return never {@literal null}
     */
    public Quarter getQuarter() {
        return quarter;
    }

    /**
     * Returns the first month of the fiscal year.
     *
     * @return never {@literal null}
     */
    public Month getStartMonth() {
        return startMonth;
    }

    /**
     * Returns the next {@link ShiftedQuarter} of the same fiscal calendar.
     *
     * @return never {@literal null}
     */
    public ShiftedQuarter next() {
        return new ShiftedQuarter(quarter.next(), startMonth);
    }

    /**
     * Returns whether the given date is part of the quarter in any year.
     *
     * @param date must not be {@literal null}
     * @return whether the day of the date lies between {@link #getStart()} and {@link #getEnd()}
     */
    public boolean contains(LocalDate date) {
        Objects.requireNonNull(date, "Reference date must not be null!");
        MonthDay start = getStart();
        MonthDay end = getEnd();
        MonthDay day = MonthDay.from(date);
        if (start.isBefore(end)) {
            return new MonthDayRange(start, end).contains(day);
        }
        return new MonthDayRange(start, LAST_DAY).contains(day) || new MonthDayRange(FIRST_DAY, end).contains(day);
    }

    /**
     * Returns the first day of the quarter.
     *
     * @return never {@literal null}
     */
    public MonthDay getStart() {
        return MonthDay.of(firstMonth(), 1);
    }

    /**
     * Returns the last day of the quarter. For a quarter ending in February this is February 29th, use
     * {@link #isLastDay(LocalDate)} or {@link #getEndDate(Year)} for a given year.
     *
     * @return never {@literal null}
     */
    public MonthDay getEnd() {
        Month lastMonth = firstMonth().plus(2);
        return MonthDay.of(lastMonth, lastMonth.maxLength());
    }

    /**
     * Returns whether the given date is the last day of the quarter, taking leap years into account.
     *
     * @param date must not be {@literal null}
     * @return whether the date is part of the quarter and the next day is not
     */
    public boolean isLastDay(LocalDate date) {
        return contains(date) && !contains(date.plusDays(1));
    }

    /**
     * Returns the first day of the quarter in the fiscal year that starts in the given calendar year.
     *
     * @param year must not be {@literal null}
     * @return never {@literal null}
     */
    public LocalDate getStartDate(Year year) {
        Objects.requireNonNull(year, "Year must not be null!");
        return quarter.getStart()
                .atYear(year.getValue())
                .plusMonths(startMonth.getValue() - 1L);
    }

    /**
     * Returns the last day of the quarter in the fiscal year that starts in the given calendar year.
     *
     * @param year must not be {@literal null}
     * @return never {@literal null}
     */
    public LocalDate getEndDate(Year year) {
        return getStartDate(year).plusMonths(3).minusDays(1);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ShiftedQuarter that)) {
            return false;
        }
        return quarter == that.quarter && startMonth == that.startMonth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(quarter, startMonth);
    }

    @Override
    public String toString() {
        return quarter + "/" + startMonth;
    }

    private Month firstMonth() {
        return quarter.getStart().getMonth().plus(startMonth.getValue() - 1L);
    }
}
