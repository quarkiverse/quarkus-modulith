package io.quarkiverse.modulith.moments;

import java.time.Month;
import java.time.MonthDay;
import java.util.Objects;

/**
 * A logical quarter of a calendar year. {@link ShiftedQuarter} moves it to a fiscal year that starts in another month.
 */
public enum Quarter {

    /**
     * January 1st to March 31st.
     */
    Q1(MonthDay.of(Month.JANUARY, 1), MonthDay.of(Month.MARCH, 31)),

    /**
     * April 1st to June 30th.
     */
    Q2(MonthDay.of(Month.APRIL, 1), MonthDay.of(Month.JUNE, 30)),

    /**
     * July 1st to September 30th.
     */
    Q3(MonthDay.of(Month.JULY, 1), MonthDay.of(Month.SEPTEMBER, 30)),

    /**
     * October 1st to December 31st.
     */
    Q4(MonthDay.of(Month.OCTOBER, 1), MonthDay.of(Month.DECEMBER, 31));

    private final MonthDay start;
    private final MonthDay end;

    Quarter(MonthDay start, MonthDay end) {
        this.start = Objects.requireNonNull(start, "Start MonthDay must not be null!");
        this.end = Objects.requireNonNull(end, "End MonthDay must not be null!");
    }

    /**
     * Returns the first day of the quarter.
     *
     * @return never {@literal null}
     */
    public MonthDay getStart() {
        return start;
    }

    /**
     * Returns the last day of the quarter.
     *
     * @return never {@literal null}
     */
    public MonthDay getEnd() {
        return end;
    }

    /**
     * Returns the next quarter, {@link #Q1} after {@link #Q4}.
     *
     * @return never {@literal null}
     */
    Quarter next() {
        Quarter[] quarters = values();
        return quarters[(ordinal() + 1) % quarters.length];
    }
}
