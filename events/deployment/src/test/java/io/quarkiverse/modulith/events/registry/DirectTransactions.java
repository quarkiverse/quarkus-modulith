package io.quarkiverse.modulith.events.registry;

import java.util.function.Supplier;

import io.quarkiverse.modulith.events.spi.TransactionRunner;

final class DirectTransactions implements TransactionRunner {

    @Override
    public <T> T callInNewTransaction(Supplier<T> work) {
        return work.get();
    }

    @Override
    public <T> T callInTransaction(Supplier<T> work) {
        return work.get();
    }
}
