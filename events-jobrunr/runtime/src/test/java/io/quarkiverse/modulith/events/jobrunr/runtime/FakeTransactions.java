package io.quarkiverse.modulith.events.jobrunr.runtime;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;

public class FakeTransactions implements TransactionSynchronizationRegistry {

    private int status = Status.STATUS_NO_TRANSACTION;
    private boolean rollbackOnly;

    public void begin() {
        status = Status.STATUS_ACTIVE;
    }

    @Override
    public Object getTransactionKey() {
        return null;
    }

    @Override
    public void putResource(Object key, Object value) {
        throw new UnsupportedOperationException("not used");
    }

    @Override
    public Object getResource(Object key) {
        throw new UnsupportedOperationException("not used");
    }

    @Override
    public void registerInterposedSynchronization(Synchronization sync) {
        throw new UnsupportedOperationException("not used");
    }

    @Override
    public int getTransactionStatus() {
        return status;
    }

    @Override
    public void setRollbackOnly() {
        rollbackOnly = true;
    }

    @Override
    public boolean getRollbackOnly() {
        return rollbackOnly;
    }
}
