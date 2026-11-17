package io.quarkiverse.modulith.runtime.events;

import java.util.function.BooleanSupplier;

import jakarta.transaction.Transactional.TxType;

import io.quarkiverse.modulith.events.spi.TransactionRunner;

/**
 * The transaction integration used by the event dispatcher. The deployment processor registers
 * {@link JtaTransactionSupport} when {@code quarkus-narayana-jta} is present, and {@link NoTransactionSupport}
 * otherwise.
 */
public interface TransactionSupport extends TransactionRunner {

    /**
     * @return whether the current thread runs inside a transaction
     */
    boolean isActive();

    /**
     * Runs the action after the current transaction commits. The action is dropped if the transaction rolls back.
     */
    void afterCommit(Runnable action);

    /**
     * Runs the work in a new transaction, suspending the current one if needed. The transaction is rolled back if the
     * work throws.
     */
    void inNewTransaction(Runnable work);

    /**
     * Runs the work in a new transaction and returns its result.
     */
    boolean inNewTransaction(BooleanSupplier work);

    /**
     * Runs a listener with the transaction semantics it declares.
     *
     * @param propagation the declared propagation, never {@code MANDATORY} (rejected at build time)
     */
    void run(TxType propagation, Runnable work);
}
