package io.quarkiverse.modulith.moments.support;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Objects;

import io.quarkiverse.modulith.moments.ShiftedQuarter;

/**
 * The settings {@link Moments} works with. The extension builds them from the {@code quarkus.modulith.moments.*}
 * configuration, applications that create a {@link Moments} or {@link TimeMachine} by hand (in plain unit tests, for
 * example) start from {@link #defaults()}.
 */
public final class MomentsProperties {

    private final Granularity granularity;
    private final ZoneId zoneId;
    private final Locale locale;
    private final Month quarterStartMonth;
    private final boolean enableTimeMachine;
    private final ShiftedQuarters quarters;

    private MomentsProperties(Granularity granularity, ZoneId zoneId, Locale locale, Month quarterStartMonth,
            boolean enableTimeMachine) {
        this.granularity = Objects.requireNonNull(granularity, "Granularity must not be null!");
        this.zoneId = Objects.requireNonNull(zoneId, "ZoneId must not be null!");
        this.locale = Objects.requireNonNull(locale, "Locale must not be null!");
        this.quarterStartMonth = Objects.requireNonNull(quarterStartMonth, "Quarter start month must not be null!");
        this.enableTimeMachine = enableTimeMachine;
        this.quarters = ShiftedQuarters.of(quarterStartMonth);
    }

    /**
     * Creates {@link MomentsProperties} from explicit values.
     *
     * @param granularity the finest events to publish, must not be {@literal null}
     * @param zoneId the zone used to compute local times and dates, must not be {@literal null}
     * @param locale the locale that defines weeks, must not be {@literal null}
     * @param quarterStartMonth the first month of the fiscal year, must not be {@literal null}
     * @param enableTimeMachine whether the {@link Moments} instance is a {@link TimeMachine}
     * @return the properties
     */
    public static MomentsProperties of(Granularity granularity, ZoneId zoneId, Locale locale, Month quarterStartMonth,
            boolean enableTimeMachine) {
        return new MomentsProperties(granularity, zoneId, locale, quarterStartMonth, enableTimeMachine);
    }

    /**
     * The defaults of Spring Modulith: hourly events, UTC, the default {@link Locale}, quarters starting in January, no
     * time machine.
     *
     * @return the default properties
     */
    public static MomentsProperties defaults() {
        return of(Granularity.HOURS, ZoneOffset.UTC, Locale.getDefault(), Month.JANUARY, false);
    }

    /**
     * The finest events to publish.
     *
     * @return never {@literal null}
     */
    public Granularity getGranularity() {
        return granularity;
    }

    /**
     * The zone used to compute the local times and dates attached to the events.
     *
     * @return never {@literal null}
     */
    public ZoneId getZoneId() {
        return zoneId;
    }

    /**
     * The locale that defines the first day of a week and the week numbering.
     *
     * @return never {@literal null}
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * The first month of the fiscal year.
     *
     * @return never {@literal null}
     */
    public Month getQuarterStartMonth() {
        return quarterStartMonth;
    }

    /**
     * Whether the {@link Moments} instance is a {@link TimeMachine}.
     *
     * @return {@code true} if time can be shifted
     */
    public boolean isEnableTimeMachine() {
        return enableTimeMachine;
    }

    /**
     * Returns the {@link ShiftedQuarter} the given date is part of.
     *
     * @param reference must not be {@literal null}
     * @return never {@literal null}
     */
    public ShiftedQuarter getShiftedQuarter(LocalDate reference) {
        Objects.requireNonNull(reference, "Reference date must not be null!");
        return quarters.getCurrent(reference);
    }

    /**
     * Returns a copy with the given granularity.
     *
     * @param granularity must not be {@literal null}
     * @return a new instance
     */
    public MomentsProperties withGranularity(Granularity granularity) {
        return of(granularity, zoneId, locale, quarterStartMonth, enableTimeMachine);
    }

    /**
     * Returns a copy with the given zone.
     *
     * @param zoneId must not be {@literal null}
     * @return a new instance
     */
    public MomentsProperties withZoneId(ZoneId zoneId) {
        return of(granularity, zoneId, locale, quarterStartMonth, enableTimeMachine);
    }

    /**
     * Returns a copy with the given locale.
     *
     * @param locale must not be {@literal null}
     * @return a new instance
     */
    public MomentsProperties withLocale(Locale locale) {
        return of(granularity, zoneId, locale, quarterStartMonth, enableTimeMachine);
    }

    /**
     * Returns a copy with the given first month of the fiscal year.
     *
     * @param quarterStartMonth must not be {@literal null}
     * @return a new instance
     */
    public MomentsProperties withQuarterStartMonth(Month quarterStartMonth) {
        return of(granularity, zoneId, locale, quarterStartMonth, enableTimeMachine);
    }

    @Override
    public String toString() {
        return "MomentsProperties[granularity=" + granularity + ", zoneId=" + zoneId + ", locale="
                + locale.toLanguageTag() + ", quarterStartMonth=" + quarterStartMonth + ", enableTimeMachine="
                + enableTimeMachine + "]";
    }
}
