package io.quarkiverse.modulith.events.jms.runtime;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public class CountingExecutor implements Executor {

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
