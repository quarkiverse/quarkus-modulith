package io.quarkiverse.modulith.events.mongodb.deployment;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

/**
 * Runs an action after the JTA transaction committed, the way the dispatcher's after-commit hook does.
 */
public final class RunAfterCommit implements Synchronization {

    private final Runnable action;

    public RunAfterCommit(Runnable action) {
        this.action = action;
    }

    @Override
    public void beforeCompletion() {
        // nothing to do before completion
    }

    @Override
    public void afterCompletion(int status) {
        if (status == Status.STATUS_COMMITTED) {
            action.run();
        }
    }
}
