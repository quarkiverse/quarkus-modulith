package io.quarkiverse.modulith.runtime.events;

import java.time.Duration;

/**
 * Runs an action the first time the shutdown timeout is read, which the dispatcher does after it stopped taking
 * deliveries, to submit a delivery while the shutdown waits.
 */
final class DuringShutdownConfig implements ListenerConfig {

    private final Runnable action;
    private boolean ran;

    DuringShutdownConfig(Runnable action) {
        this.action = action;
    }

    @Override
    public Duration shutdownTimeout() {
        if (!ran) {
            ran = true;
            action.run();
        }
        return Duration.ZERO;
    }

    @Override
    public CompletionTiming completionTiming() {
        return CompletionTiming.IN_LISTENER;
    }
}
