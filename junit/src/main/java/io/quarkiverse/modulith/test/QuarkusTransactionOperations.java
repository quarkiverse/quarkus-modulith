package io.quarkiverse.modulith.test;

import java.util.function.Supplier;

import io.quarkus.narayana.jta.QuarkusTransaction;

/**
 * Runs each callback in a new JTA transaction.
 */
final class QuarkusTransactionOperations implements TransactionOperations {

    @Override
    public <T> T execute(Supplier<T> callback) {
        return QuarkusTransaction.requiringNew().call(callback::get);
    }
}
