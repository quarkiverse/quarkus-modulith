package io.quarkiverse.modulith.moments;

import java.time.MonthDay;

/**
 * An inclusive range of days within a calendar year.
 *
 * @param start the first day, inclusive
 * @param end the last day, inclusive
 */
record MonthDayRange(MonthDay start, MonthDay end) {

    boolean contains(MonthDay day) {
        return !day.isBefore(start) && !day.isAfter(end);
    }
}
