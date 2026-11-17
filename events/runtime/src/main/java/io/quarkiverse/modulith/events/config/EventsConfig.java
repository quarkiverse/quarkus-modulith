package io.quarkiverse.modulith.events.config;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration of the event publication registry, shared by every store (JPA, JDBC, MongoDB, Neo4j). Store-specific
 * settings live under {@code quarkus.modulith.events.<store>}. The completion mode is in {@link EventsFixedConfig}.
 * <p>
 * Stores read this mapping, so it is part of the stable surface of {@code quarkus-modulith-events}.
 */
@ConfigMapping(prefix = "quarkus.modulith.events")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface EventsConfig {

    /**
     * Whether to submit all incomplete publications again when the application starts. The resubmission runs in the
     * background after startup, in batches. With several instances of the application, prefer
     * {@code republish-interval} with {@code republish-older-than}, since every instance would resubmit the same
     * publications at its start.
     */
    @WithDefault("false")
    boolean republishOutstandingEventsOnRestart();

    /**
     * How often to submit incomplete publications again. Disabled when not set. Needs {@code quarkus-scheduler}.
     */
    Optional<Duration> republishInterval();

    /**
     * Only publications older than this are submitted again by the periodic job, so that deliveries still in flight
     * are left alone. Without {@code staleness.processing} and {@code staleness.resubmitted}, it is also the age after
     * which a resubmission takes over a claimed or processing publication, whose instance probably died.
     */
    @WithDefault("5m")
    Duration republishOlderThan();

    /**
     * How many publications a resubmission reads from the store at once. Must be positive.
     */
    @WithDefault("100")
    int republishBatchSize();

    /**
     * How often to delete completed publications. Disabled when not set. Needs {@code quarkus-scheduler}.
     */
    Optional<Duration> deleteCompletedInterval();

    /**
     * Completed publications whose completion is older than this are deleted by the periodic job.
     */
    @WithDefault("7d")
    Duration deleteCompletedOlderThan();

    /**
     * Marks publications that stay in one state too long as failed.
     */
    Staleness staleness();

    /**
     * Resubmission limits.
     */
    Resubmission resubmission();

    /**
     * Staleness thresholds per status, like Spring Modulith's {@code spring.modulith.events.staleness}. A status
     * without threshold is not monitored. Thresholds are compared with the last publication date (the last
     * resubmission, or else the publication).
     */
    interface Staleness {

        /**
         * After how long a {@code PUBLISHED} publication is stale. Must be positive.
         */
        Optional<Duration> published();

        /**
         * After how long a {@code PROCESSING} publication is stale. Must be positive. A resubmission takes over a
         * claimed or processing publication older than this. The monitor also marks the outbox publications of event
         * externalization failed, so keep it longer than {@code externalization.outbox.send-timeout} plus
         * {@code stale-after}, or another instance may send a publication that is still being sent.
         */
        Optional<Duration> processing();

        /**
         * After how long a {@code RESUBMITTED} publication is stale. Must be positive.
         */
        Optional<Duration> resubmitted();

        /**
         * How often to look for stale publications. Must be positive. Needs {@code quarkus-scheduler}.
         */
        @WithDefault("1m")
        Duration checkInterval();
    }

    /**
     * What happens to a publication once its listener succeeded, like Spring Modulith's {@code CompletionMode}. The
     * mode itself is {@link EventsFixedConfig#completionMode()}, fixed at build time and readable at runtime.
     */
    enum CompletionMode {

        /**
         * Sets the completion date and status and keeps the publication.
         */
        UPDATE,

        /**
         * Removes the publication.
         */
        DELETE,

        /**
         * Moves the publication to an archive (for JPA and JDBC the {@code event_publication_archive} table). Every
         * query about completed publications reads the archive.
         */
        ARCHIVE
    }

    /**
     * Resubmission limits.
     */
    interface Resubmission {

        /**
         * Publications with at least this many completion attempts are abandoned: they are no longer resubmitted and
         * are listed by {@code AbandonedEventPublications}. Never when not set.
         */
        OptionalInt abandonAfterAttempts();
    }
}
