package io.quarkiverse.modulith.events.externalization.runtime;

import jakarta.inject.Singleton;

/**
 * Used when the application has no transaction manager.
 */
@Singleton
public class NoExternalizationTransactions implements ExternalizationTransactions {

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public void afterCommit(Runnable work) {
        work.run();
    }
}
