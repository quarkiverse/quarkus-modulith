package io.quarkiverse.modulith.test;

import java.util.function.Supplier;

/**
 * Runs callbacks without a transaction, for applications without {@code quarkus-narayana-jta}.
 */
final class DirectTransactionOperations implements TransactionOperations {

    @Override
    public <T> T execute(Supplier<T> callback) {
        return callback.get();
    }
}
