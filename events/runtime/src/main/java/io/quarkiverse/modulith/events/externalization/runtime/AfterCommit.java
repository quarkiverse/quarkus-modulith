package io.quarkiverse.modulith.events.externalization.runtime;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

/**
 * Runs work when a transaction committed.
 */
final class AfterCommit implements Synchronization {

    private final Runnable work;

    AfterCommit(Runnable work) {
        this.work = work;
    }

    @Override
    public void beforeCompletion() {
        // nothing to do before the commit
    }

    @Override
    public void afterCompletion(int status) {
        if (status == Status.STATUS_COMMITTED) {
            work.run();
        }
    }
}
