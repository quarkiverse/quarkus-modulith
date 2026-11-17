package io.quarkiverse.modulith.events.externalization.fakes;

import java.util.concurrent.RejectedExecutionException;

import io.quarkiverse.modulith.events.spi.ListenerExecutor;

/**
 * An executor that is shut down, or whose bounded queue is full.
 */
public class RejectingExecutor implements ListenerExecutor {

    @Override
    public void execute(Runnable command) {
        throw new RejectedExecutionException("queue full");
    }
}
