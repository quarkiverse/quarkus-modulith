package io.quarkiverse.modulith.events.spi;

/**
 * Makes the current transaction read-only for a listener declared with
 * {@code @ApplicationModuleListener(readOnlyTransaction = true)}. Provided by persistence extensions, for example
 * {@code quarkus-modulith-events-jpa} switches the Hibernate session of the transaction to read-only with manual
 * flushing. Without an implementation the attribute has no effect.
 * <p>
 * This is a service provider interface for Quarkus Modulith extensions, not for applications.
 */
public interface ReadOnlyTransactions {

    /**
     * Called inside the listener's transaction, before the listener runs.
     */
    void markReadOnly();
}
