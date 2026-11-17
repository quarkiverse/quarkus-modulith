package io.quarkiverse.modulith.runtime.events;

import java.util.ArrayList;
import java.util.List;

import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * Reports a fixed transaction status.
 */
final class FakeSynchronizationRegistry implements TransactionSynchronizationRegistry {

    final List<Synchronization> interposed = new ArrayList<>();
    int status;

    @Override
    public Object getTransactionKey() {
        return "tx";
    }

    @Override
    public void putResource(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getResource(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerInterposedSynchronization(Synchronization sync) {
        interposed.add(sync);
    }

    @Override
    public int getTransactionStatus() {
        return status;
    }

    @Override
    public void setRollbackOnly() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getRollbackOnly() {
        return false;
    }
}
