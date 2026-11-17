package io.quarkiverse.modulith.events.spi;

import java.util.function.Supplier;

/**
 * Runs work in a transaction when the application has a transaction manager, and directly otherwise. Lets the event
 * publication registry and its stores work with and without {@code quarkus-narayana-jta}.
 * <p>
 * This is a service provider interface for Quarkus Modulith extensions, not for applications.
 */
public interface TransactionRunner {

    /**
     * Runs the work in a new transaction, suspending the current one if needed.
     */
    <T> T callInNewTransaction(Supplier<T> work);

    /**
     * Runs the work in the current transaction, or in a new one when there is none.
     */
    <T> T callInTransaction(Supplier<T> work);
}
