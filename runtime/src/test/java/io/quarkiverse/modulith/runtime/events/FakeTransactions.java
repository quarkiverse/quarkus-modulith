package io.quarkiverse.modulith.runtime.events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import jakarta.transaction.Transactional.TxType;

/**
 * A transaction manager stand-in that records what the dispatcher asks for.
 */
final class FakeTransactions implements TransactionSupport {

    final List<Runnable> afterCommit = new ArrayList<>();
    final List<String> calls = new ArrayList<>();
    boolean active;
    boolean activeInListener;

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void afterCommit(Runnable action) {
        afterCommit.add(action);
    }

    void commit() {
        active = false;
        afterCommit.forEach(Runnable::run);
        afterCommit.clear();
    }

    @Override
    public void inNewTransaction(Runnable work) {
        calls.add("new");
        work.run();
    }

    @Override
    public boolean inNewTransaction(BooleanSupplier work) {
        calls.add("new");
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
        calls.add(propagation.name());
        boolean before = active;
        active = activeInListener;
        try {
            work.run();
        } finally {
            active = before;
        }
    }
}
