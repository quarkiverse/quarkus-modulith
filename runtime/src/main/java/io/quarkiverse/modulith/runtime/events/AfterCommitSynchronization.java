package io.quarkiverse.modulith.runtime.events;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

/**
 * Runs an action once the transaction it is registered with has committed.
 *
 * @param action the action
 */
record AfterCommitSynchronization(Runnable action) implements Synchronization {

    @Override
    public void beforeCompletion() {
        // nothing to do before the commit
    }

    @Override
    public void afterCompletion(int status) {
        if (status == Status.STATUS_COMMITTED) {
            action.run();
        }
    }
}
