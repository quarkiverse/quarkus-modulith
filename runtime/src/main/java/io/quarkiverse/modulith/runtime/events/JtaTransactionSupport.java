package io.quarkiverse.modulith.runtime.events;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.Transactional.TxType;

import io.quarkus.narayana.jta.QuarkusTransaction;

/**
 * Transaction integration backed by Narayana.
 */
@Singleton
public class JtaTransactionSupport implements TransactionSupport {

    @Inject
    TransactionSynchronizationRegistry synchronizations;

    @Override
    public boolean isActive() {
        int status = synchronizations.getTransactionStatus();
        return status == Status.STATUS_ACTIVE || status == Status.STATUS_MARKED_ROLLBACK;
    }

    /**
     * A transaction marked for rollback will not commit, and Narayana refuses new synchronizations for it, so the
     * action is dropped right away instead of failing the code that fired the event.
     */
    @Override
    public void afterCommit(Runnable action) {
        if (synchronizations.getTransactionStatus() != Status.STATUS_MARKED_ROLLBACK) {
            synchronizations.registerInterposedSynchronization(new AfterCommitSynchronization(action));
        }
    }

    @Override
    public void inNewTransaction(Runnable work) {
        QuarkusTransaction.requiringNew().run(work);
    }

    @Override
    public boolean inNewTransaction(BooleanSupplier work) {
        return QuarkusTransaction.requiringNew().call(work::getAsBoolean);
    }

    @Override
    public <T> T callInNewTransaction(Supplier<T> work) {
        return QuarkusTransaction.requiringNew().call(work::get);
    }

    @Override
    public <T> T callInTransaction(Supplier<T> work) {
        return QuarkusTransaction.joiningExisting().call(work::get);
    }

    /**
     * Listeners run after the publishing transaction completed, so there is normally no transaction to join:
     * {@code REQUIRED} and {@code REQUIRES_NEW} start one. {@code SUPPORTS}, {@code NOT_SUPPORTED} and {@code NEVER}
     * run without a transaction, suspending one that is still associated with the thread.
     */
    @Override
    public void run(TxType propagation, Runnable work) {
        if (propagation == TxType.REQUIRED || propagation == TxType.REQUIRES_NEW) {
            QuarkusTransaction.requiringNew().run(work);
        } else {
            QuarkusTransaction.suspendingExisting().run(work);
        }
    }
}
