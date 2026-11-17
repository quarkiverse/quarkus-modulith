package io.quarkiverse.modulith.events.externalization.fakes;

import java.util.ArrayList;
import java.util.List;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;

public class FakeRegistry implements TransactionSynchronizationRegistry {

    private final List<Synchronization> synchronizations = new ArrayList<>();
    private int status = Status.STATUS_NO_TRANSACTION;

    public void status(int value) {
        status = value;
    }

    public List<Synchronization> synchronizations() {
        return List.copyOf(synchronizations);
    }

    @Override
    public int getTransactionStatus() {
        return status;
    }

    @Override
    public void registerInterposedSynchronization(Synchronization synchronization) {
        synchronizations.add(synchronization);
    }

    @Override
    public Object getTransactionKey() {
        throw new UnsupportedOperationException();
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
    public void setRollbackOnly() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getRollbackOnly() {
        throw new UnsupportedOperationException();
    }
}
