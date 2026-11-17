package io.quarkiverse.modulith.moments.runtime;

import java.time.Month;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

import io.quarkiverse.modulith.moments.support.Granularity;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Passage of time events. The settings take effect when the application starts.
 */
@ConfigMapping(prefix = "quarkus.modulith.moments")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface MomentsConfig {

    /**
     * The finest events to publish: {@code seconds}, {@code minutes}, {@code hours} or {@code days}. Every granularity
     * includes the coarser ones, so {@code hours} publishes hourly and daily events (plus the weekly, monthly, quarterly
     * and yearly ones derived from the days). Decides which scheduler jobs are registered at startup and how
     * {@code TimeMachine.shiftBy} steps through time. Spring Modulith: {@code spring.modulith.moments.granularity}.
     */
    @WithDefault("hours")
    Granularity granularity();

    /**
     * The zone used to compute the local times and dates attached to the events, and the zone of the scheduler jobs, so
     * the daily job runs at midnight of this zone. Spring Modulith: {@code spring.modulith.moments.zone-id}.
     */
    @WithDefault("UTC")
    ZoneId zoneId();

    /**
     * The locale that defines the first day of a week and the week numbering, for example {@code en-US} (weeks from
     * Sunday to Saturday) or {@code de-DE} (ISO weeks from Monday to Sunday). Defaults to the default locale of the
     * JVM, which a native executable fixes at build time, so set it explicitly. A native executable only has the data
     * of the locales listed in {@code quarkus.locales} (by default the locale of the build machine): add this locale
     * there, for example {@code quarkus.locales=en-US,de-DE}. The extension logs a warning at startup when the locale
     * is not available. Spring Modulith: {@code spring.modulith.moments.locale}.
     */
    Optional<Locale> locale();

    /**
     * The first month of the fiscal year, so of the first quarter. With {@code april}, the first quarter runs from April
     * to June and the fourth from January to March. Spring Modulith: {@code spring.modulith.moments.quarter-start-month}.
     */
    @WithDefault("january")
    Month quarterStartMonth();

    /**
     * The scheduler jobs that publish the events.
     */
    Jobs jobs();

    interface Jobs {

        /**
         * Whether this instance registers the scheduler jobs that publish the events. Every instance with the jobs
         * publishes every event, so with several instances of the application each event arrives once per instance.
         * Set it to {@code false} on all instances but one (for example with the environment variable
         * {@code QUARKUS_MODULITH_MOMENTS_JOBS_ENABLED=false}), or make the listeners idempotent. The time machine
         * works either way. Quarkus extra.
         */
        @WithDefault("true")
        boolean enabled();
    }
}
