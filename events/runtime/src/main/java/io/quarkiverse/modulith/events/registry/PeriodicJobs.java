package io.quarkiverse.modulith.events.registry;

import java.time.Duration;

/**
 * Schedules the registry's maintenance jobs. Implemented on top of {@code quarkus-scheduler} when it is present.
 */
public interface PeriodicJobs {

    /**
     * Runs the task every interval, skipping a run while the previous one is still going.
     */
    void schedule(String identity, Duration interval, Runnable task);
}
