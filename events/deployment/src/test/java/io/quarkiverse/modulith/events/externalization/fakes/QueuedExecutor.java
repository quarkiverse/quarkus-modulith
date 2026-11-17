package io.quarkiverse.modulith.events.externalization.fakes;

import java.util.ArrayList;
import java.util.List;

import io.quarkiverse.modulith.events.spi.ListenerExecutor;

/**
 * Keeps the tasks until the test runs them, in any order, like worker threads that start in any order.
 */
public class QueuedExecutor implements ListenerExecutor {

    private final List<Runnable> tasks = new ArrayList<>();

    @Override
    public void execute(Runnable command) {
        tasks.add(command);
    }

    /**
     * Runs the task queued at the given position, counted from the first.
     */
    public void run(int position) {
        tasks.get(position).run();
    }

    public int queued() {
        return tasks.size();
    }
}
