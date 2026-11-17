package io.quarkiverse.modulith.events.jobrunr.runtime;

import java.util.concurrent.atomic.AtomicInteger;

import io.quarkiverse.modulith.events.spi.ListenerExecutor;

public class CountingListenerExecutor implements ListenerExecutor {

    final AtomicInteger executed = new AtomicInteger();

    @Override
    public void execute(Runnable command) {
        executed.incrementAndGet();
        command.run();
    }
}
