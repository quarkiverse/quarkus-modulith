package io.quarkiverse.modulith.test;

import java.util.function.Supplier;

/**
 * Runs code in a new transaction, the counterpart of the {@code TransactionOperations} a Spring Modulith
 * {@link Scenario} stimulus receives. With {@code quarkus-narayana-jta} each call runs in a new transaction (like
 * {@code QuarkusTransaction.requiringNew()}), committed when the code returns and rolled back when it throws. Without
 * it the code runs as is.
 */
public interface TransactionOperations {

    /**
     * @return the value the callback returns
     */
    <T> T execute(Supplier<T> callback);

    /**
     * Runs the callback in a new transaction.
     */
    default void executeWithoutResult(Runnable callback) {
        execute(() -> {
            callback.run();
            return null;
        });
    }

    /**
     * @return transaction operations for the running application
     */
    static TransactionOperations create() {
        return create(ClassPresence.isPresent(ClassPresence.NARAYANA));
    }

    static TransactionOperations create(boolean narayanaPresent) {
        return narayanaPresent ? new QuarkusTransactionOperations() : new DirectTransactionOperations();
    }
}
