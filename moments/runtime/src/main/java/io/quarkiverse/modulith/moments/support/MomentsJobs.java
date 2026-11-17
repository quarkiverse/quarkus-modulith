package io.quarkiverse.modulith.moments.support;

import java.util.Locale;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.quarkus.scheduler.Scheduler;

/**
 * Registers one scheduler job per unit of the configured {@link Granularity} when the application starts. The jobs use
 * Quartz cron expressions in the configured zone, so the daily job runs at midnight of that zone, and skip an
 * execution while the previous one still runs.
 * <p>
 * Every instance of the application registers the jobs, so in a cluster each instance publishes every event, unless
 * {@value #JOBS_ENABLED} is {@code false} on it.
 */
@Singleton
class MomentsJobs {

    static final String JOB_PREFIX = "modulith-moments-";

    /**
     * Declared and documented in {@code MomentsConfig}, read here by name to keep this package free of the
     * configuration package.
     */
    static final String JOBS_ENABLED = "quarkus.modulith.moments.jobs.enabled";

    private static final Logger LOG = Logger.getLogger(MomentsJobs.class);

    @Inject
    Moments moments;

    @Inject
    Scheduler scheduler;

    @ConfigProperty(name = JOBS_ENABLED, defaultValue = "true")
    boolean jobsEnabled;

    void schedule(@Observes @Priority(Interceptor.Priority.LIBRARY_AFTER) StartupEvent event) {
        if (!jobsEnabled) {
            LOG.infof("%s is false, so this instance publishes no passage of time events on its own", JOBS_ENABLED);
            return;
        }
        if (!scheduler.isStarted()) {
            LOG.info("The scheduler is not running, so Moments publishes no passage of time events on its own");
            return;
        }
        MomentsProperties properties = moments.getProperties();
        Granularity granularity = properties.getGranularity();
        String zone = properties.getZoneId().getId();
        schedule(granularity, Granularity.SECONDS, "* * * * * ?", zone, moments::everySecond);
        schedule(granularity, Granularity.MINUTES, "0 * * * * ?", zone, moments::everyMinute);
        schedule(granularity, Granularity.HOURS, "0 0 * * * ?", zone, moments::everyHour);
        schedule(granularity, Granularity.DAYS, "0 0 0 * * ?", zone, moments::everyMidnight);
        LOG.infof("Moments publishes the passage of time events from this instance. With several instances, set %s"
                + " to false on all but one, or every instance publishes every event", JOBS_ENABLED);
    }

    private void schedule(Granularity configured, Granularity unit, String cron, String zone, Runnable tick) {
        if (configured.includes(unit)) {
            String identity = JOB_PREFIX + unit.name().toLowerCase(Locale.ROOT);
            scheduler.newJob(identity)
                    .setCron(cron)
                    .setTimeZone(zone)
                    .setConcurrentExecution(ConcurrentExecution.SKIP)
                    .setDescription("Publishes the passage of time events of Quarkus Modulith Moments")
                    .setTask(new MomentsJob(tick))
                    .schedule();
            LOG.debugf("Scheduled %s with cron %s in zone %s", identity, cron, zone);
        }
    }
}
