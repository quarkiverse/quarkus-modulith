package io.quarkiverse.modulith.runtime.events;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Runs tasks on the calling thread, records under which name, or rejects them.
 */
final class RecordingExecutorService extends AbstractExecutorService {

    private final String name;
    private final List<String> calls;
    boolean reject;

    RecordingExecutorService(String name, List<String> calls) {
        this.name = name;
        this.calls = calls;
    }

    @Override
    public void execute(Runnable command) {
        if (reject) {
            calls.add(name + " rejected");
            throw new RejectedExecutionException(name + " is shut down");
        }
        calls.add(name);
        command.run();
    }

    @Override
    public void shutdown() {
        reject = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        reject = true;
        return List.of();
    }

    @Override
    public boolean isShutdown() {
        return reject;
    }

    @Override
    public boolean isTerminated() {
        return reject;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return reject;
    }
}
