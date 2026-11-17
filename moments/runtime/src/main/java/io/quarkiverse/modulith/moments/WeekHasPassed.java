package io.quarkiverse.modulith.moments;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Objects;

import org.jmolecules.event.types.DomainEvent;

/**
 * A {@link DomainEvent} published at the end of the last day of every week. Which day ends a week, and how weeks are
 * numbered, depends on the {@link Locale}: {@link Locale#US} weeks run from Sunday to Saturday,
 * {@link Locale#GERMANY} (ISO) weeks from Monday to Sunday.
 * <p>
 * The year and the week are the week-based year and the week of the week-based year of the {@link Locale}'s
 * {@link WeekFields}, so the last days of December can belong to week 1 of the next year.
 */
public class WeekHasPassed implements DomainEvent {

    private final Year year;
    private final int week;
    private final Locale locale;

    WeekHasPassed(Year year, int week, Locale locale) {
        this.year = Objects.requireNonNull(year, "Year must not be null!");
        if (week < 1 || week > 53) {
            throw new IllegalArgumentException("Week must be between 1 and 53, got " + week + "!");
        }
        this.week = week;
        this.locale = Objects.requireNonNull(locale, "Locale must not be null!");
    }

    /**
     * Creates a new {@link WeekHasPassed} for the given week-based {@link Year}, week and {@link Locale}.
     *
     * @param year the week-based year, must not be {@literal null}
     * @param week the week of the week-based year, between 1 and 53
     * @param locale the {@link Locale} whose {@link WeekFields} define the week, must not be {@literal null}
     * @return the event
     */
    public static WeekHasPassed of(Year year, int week, Locale locale) {
        return new WeekHasPassed(year, week, locale);
    }

    /**
     * Creates a new {@link WeekHasPassed} for the given week-based {@link Year} and week, using the default
     * {@link Locale}.
     *
     * @param year the week-based year, must not be {@literal null}
     * @param week the week of the week-based year, between 1 and 53
     * @return the event
     */
    public static WeekHasPassed of(Year year, int week) {
        return of(year, week, Locale.getDefault());
    }

    /**
     * The week-based year the week belongs to.
     *
     * @return never {@literal null}
     */
    public Year getYear() {
        return year;
    }

    /**
     * The week of the week-based year that has just passed.
     *
     * @return between 1 and 53
     */
    public int getWeek() {
        return week;
    }

    /**
     * The first day of the week, according to the {@link Locale}.
     *
     * @return never {@literal null}
     */
    public LocalDate getStartDate() {
        WeekFields fields = WeekFields.of(locale);
        return LocalDate.of(year.getValue(), Month.JULY, 1)
                .with(fields.weekOfWeekBasedYear(), week)
                .with(fields.dayOfWeek(), 1);
    }

    /**
     * The last day of the week, according to the {@link Locale}.
     *
     * @return never {@literal null}
     */
    public LocalDate getEndDate() {
        return getStartDate().plusDays(6);
    }

    Locale getLocale() {
        return locale;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WeekHasPassed that)) {
            return false;
        }
        return week == that.week
                && Objects.equals(year, that.year)
                && Objects.equals(locale, that.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locale, week, year);
    }

    @Override
    public String toString() {
        return "WeekHasPassed[" + year + "-W" + week + ", " + locale.toLanguageTag() + "]";
    }
}
