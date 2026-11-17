package io.quarkiverse.modulith.moments.support;

import java.util.Objects;
import java.util.function.Consumer;

import io.quarkus.scheduler.ScheduledExecution;

/**
 * A scheduler task that runs one of the periodic methods of {@link Moments}.
 */
final class MomentsJob implements Consumer<ScheduledExecution> {

    private final Runnable tick;

    MomentsJob(Runnable tick) {
        this.tick = Objects.requireNonNull(tick, "Tick must not be null!");
    }

    @Override
    public void accept(ScheduledExecution execution) {
        tick.run();
    }
}
