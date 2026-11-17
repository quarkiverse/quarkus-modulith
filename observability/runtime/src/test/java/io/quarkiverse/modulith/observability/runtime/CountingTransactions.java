package io.quarkiverse.modulith.observability.runtime;

import java.util.function.Supplier;

import io.quarkiverse.modulith.events.spi.TransactionRunner;

/**
 * Runs work directly and counts the new transactions asked for.
 */
final class CountingTransactions implements TransactionRunner {

    int started;

    @Override
    public <T> T callInNewTransaction(Supplier<T> work) {
        started++;
        return work.get();
    }

    @Override
    public <T> T callInTransaction(Supplier<T> work) {
        throw new UnsupportedOperationException("The gauges count in new transactions");
    }
}
