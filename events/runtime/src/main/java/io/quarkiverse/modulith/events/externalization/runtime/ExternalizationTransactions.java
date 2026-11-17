package io.quarkiverse.modulith.events.externalization.runtime;

/**
 * The transaction support externalization needs: whether a transaction is active and work after its commit. Backed by
 * JTA when {@code quarkus-narayana-jta} is present.
 */
public interface ExternalizationTransactions {

    /**
     * @return whether the current thread runs in a transaction
     */
    boolean isActive();

    /**
     * Runs the work after the current transaction committed, and not at all when it rolls back. Without a transaction
     * the work runs right away.
     */
    void afterCommit(Runnable work);
}
