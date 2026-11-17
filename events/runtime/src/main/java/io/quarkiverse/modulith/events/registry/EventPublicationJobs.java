package io.quarkiverse.modulith.events.registry;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.quarkiverse.modulith.events.config.EventsConfig;
import io.quarkus.runtime.StartupEvent;

/**
 * Submits incomplete publications again after startup, and schedules the periodic jobs that are configured:
 * resubmission, deletion of completed publications and the staleness monitor. Startup itself only validates the
 * configuration and registers jobs; the resubmission of the backlog runs in the background, page by page.
 */
@Singleton
public class EventPublicationJobs {

    static final String LEGACY_REPUBLISH_KEY = "quarkus.modulith.republish-outstanding-events-on-restart";
    private static final Logger LOG = Logger.getLogger(EventPublicationJobs.class);

    @Inject
    DefaultEventPublicationRegistry registry;

    @Inject
    EventsConfig config;

    @Inject
    Config rawConfig;

    @Inject
    Instance<PeriodicJobs> jobs;

    @Inject
    ExecutorService executor;

    final Set<String> failing = ConcurrentHashMap.newKeySet();

    void onStart(@Observes StartupEvent event) {
        validateConfig();
        if (republishOnRestart()) {
            runInBackground(() -> run("submitting incomplete publications again at startup",
                    () -> registry.resubmitIncompletePublications(publication -> true)));
        }
        schedule("quarkus-modulith-republish", config.republishInterval(),
                () -> registry.resubmitIncompletePublicationsOlderThan(config.republishOlderThan()));
        schedule("quarkus-modulith-delete-completed", config.deleteCompletedInterval(),
                () -> registry.deletePublicationsOlderThan(config.deleteCompletedOlderThan()));
        EventsConfig.Staleness staleness = config.staleness();
        boolean monitored = staleness.published().isPresent() || staleness.processing().isPresent()
                || staleness.resubmitted().isPresent();
        schedule("quarkus-modulith-staleness", monitored ? Optional.of(staleness.checkInterval()) : Optional.empty(),
                registry::markStalePublicationsFailed);
    }

    /**
     * Honors the key Spring Modulith used before 1.3, mapped to the Quarkus prefix, with a warning.
     */
    boolean republishOnRestart() {
        Optional<Boolean> legacy = legacyRepublishOnRestart();
        legacy.ifPresent(
                value -> LOG.warnf("%s is deprecated, use quarkus.modulith.events.republish-outstanding-events-on-restart",
                        LEGACY_REPUBLISH_KEY));
        return config.republishOutstandingEventsOnRestart() || legacy.orElse(false);
    }

    Optional<Boolean> legacyRepublishOnRestart() {
        return rawConfig.getOptionalValue(LEGACY_REPUBLISH_KEY, Boolean.class);
    }

    Optional<PeriodicJobs> periodicJobs() {
        return jobs.stream().findFirst();
    }

    void runInBackground(Runnable task) {
        executor.execute(task);
    }

    /**
     * Fails the startup on settings that would only fail later, in every run of a job.
     */
    void validateConfig() {
        EventsConfig.Staleness staleness = config.staleness();
        for (Optional<Duration> threshold : List.of(staleness.published(), staleness.processing(),
                staleness.resubmitted())) {
            threshold.filter(EventPublicationJobs::isNotPositive).ifPresent(duration -> {
                throw new IllegalArgumentException(
                        "quarkus.modulith.events.staleness thresholds must be positive, got " + duration);
            });
        }
        if (isNotPositive(staleness.checkInterval())) {
            throw new IllegalArgumentException("quarkus.modulith.events.staleness.check-interval must be positive, got "
                    + staleness.checkInterval());
        }
        if (config.republishBatchSize() < 1) {
            throw new IllegalArgumentException("quarkus.modulith.events.republish-batch-size must be at least 1, got "
                    + config.republishBatchSize());
        }
    }

    private static boolean isNotPositive(Duration duration) {
        return duration.isNegative() || duration.isZero();
    }

    private void schedule(String identity, Optional<Duration> interval, Runnable task) {
        if (interval.isEmpty()) {
            return;
        }
        Optional<PeriodicJobs> scheduler = periodicJobs();
        if (scheduler.isEmpty()) {
            LOG.warnf("Job %s is configured but no scheduler runs it: add quarkus-scheduler, and set the interval in"
                    + " application.properties, where the build sees it", identity);
            return;
        }
        scheduler.get().schedule(identity, interval.get(), () -> run(identity, task));
    }

    /**
     * Runs a job so the schedule stays alive. The first failure of a job is logged at ERROR, the following ones at
     * DEBUG until the job succeeds again, so that a store outage does not flood the log.
     */
    void run(String description, Runnable task) {
        try {
            task.run();
            if (failing.remove(description)) {
                LOG.infof("Recovered %s", description);
            }
        } catch (RuntimeException e) {
            if (failing.add(description)) {
                LOG.errorf(e, "Failed %s, logging further failures at DEBUG until it recovers", description);
            } else {
                LOG.debugf(e, "Failed %s again", description);
            }
        }
    }
}
