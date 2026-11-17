package io.quarkiverse.modulith.events.registry;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import io.quarkus.scheduler.Scheduler;

/**
 * {@link PeriodicJobs} on the Quarkus scheduler. Registered only when {@code quarkus-scheduler} is present and the
 * build sees a job interval or a staleness threshold, since injecting the scheduler starts it.
 */
@Singleton
public class SchedulerPeriodicJobs implements PeriodicJobs {

    @Inject
    Scheduler scheduler;

    @Override
    public void schedule(String identity, Duration interval, Runnable task) {
        scheduler.newJob(identity)
                .setInterval(interval.toString())
                .setConcurrentExecution(ConcurrentExecution.SKIP)
                .setTask(execution -> task.run())
                .schedule();
    }
}
