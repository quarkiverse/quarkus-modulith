package io.quarkiverse.modulith.test;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Lets Awaitility poll on the application's executor. Awaitility rejects scheduled executors, which the Quarkus
 * executor is, so this adapter only forwards {@link #execute(Runnable)}. The application owns the executor, so the
 * lifecycle methods do not stop it.
 */
final class PollingExecutor extends AbstractExecutorService {

    private final Executor delegate;

    PollingExecutor(Executor delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(command);
    }

    @Override
    public void shutdown() {
        // the application shuts its executor down
    }

    @Override
    public List<Runnable> shutdownNow() {
        return List.of();
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return false;
    }
}
