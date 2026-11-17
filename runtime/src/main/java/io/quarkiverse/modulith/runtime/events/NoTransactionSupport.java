package io.quarkiverse.modulith.runtime.events;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import jakarta.inject.Singleton;
import jakarta.transaction.Transactional.TxType;

/**
 * Used when the application has no transaction manager. Listeners run right after the event is fired.
 */
@Singleton
public class NoTransactionSupport implements TransactionSupport {

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public void afterCommit(Runnable action) {
        action.run();
    }

    @Override
    public void inNewTransaction(Runnable work) {
        work.run();
    }

    @Override
    public boolean inNewTransaction(BooleanSupplier work) {
        return work.getAsBoolean();
    }

    @Override
    public <T> T callInNewTransaction(Supplier<T> work) {
        return work.get();
    }

    @Override
    public <T> T callInTransaction(Supplier<T> work) {
        return work.get();
    }

    @Override
    public void run(TxType propagation, Runnable work) {
        work.run();
    }
}
