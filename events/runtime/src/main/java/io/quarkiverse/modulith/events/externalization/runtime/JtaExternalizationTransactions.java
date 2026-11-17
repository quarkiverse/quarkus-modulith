package io.quarkiverse.modulith.events.externalization.runtime;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * JTA transactions through the {@link TransactionSynchronizationRegistry} of {@code quarkus-narayana-jta}.
 */
@Singleton
public class JtaExternalizationTransactions implements ExternalizationTransactions {

    @Inject
    TransactionSynchronizationRegistry registry;

    @Override
    public boolean isActive() {
        return registry.getTransactionStatus() != Status.STATUS_NO_TRANSACTION;
    }

    @Override
    public void afterCommit(Runnable work) {
        if (isActive()) {
            registry.registerInterposedSynchronization(new AfterCommit(work));
        } else {
            work.run();
        }
    }
}
