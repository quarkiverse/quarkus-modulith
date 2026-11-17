package io.quarkiverse.modulith.moments.deployment.fixtures;

import java.util.List;

import io.quarkus.scheduler.Scheduler;
import io.quarkus.scheduler.Trigger;

/**
 * Reads the identities of the scheduled jobs.
 */
public final class Jobs {

    private Jobs() {
    }

    public static List<String> identities(Scheduler scheduler) {
        return scheduler.getScheduledJobs().stream().map(Trigger::getId).sorted().toList();
    }
}
