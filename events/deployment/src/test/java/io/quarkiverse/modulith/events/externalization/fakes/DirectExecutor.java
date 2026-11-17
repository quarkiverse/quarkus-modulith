package io.quarkiverse.modulith.events.externalization.fakes;

import java.util.concurrent.atomic.AtomicInteger;

import io.quarkiverse.modulith.events.spi.ListenerExecutor;

public class DirectExecutor implements ListenerExecutor {

    private final AtomicInteger executed = new AtomicInteger();

    @Override
    public void execute(Runnable command) {
        executed.incrementAndGet();
        command.run();
    }

    public int executed() {
        return executed.get();
    }
}
