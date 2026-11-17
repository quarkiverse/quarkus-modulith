package io.quarkiverse.modulith.events.mongodb.runtime;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

import com.mongodb.client.ClientSession;

/**
 * Commits or aborts the MongoDB transaction of a client session with the JTA transaction it is bound to, the way
 * MongoDB with Panache does.
 */
public final class MongoSessionSynchronization implements Synchronization {

    private final ClientSession session;

    public MongoSessionSynchronization(ClientSession session) {
        this.session = session;
    }

    @Override
    public void beforeCompletion() {
        // the MongoDB transaction completes after the JTA transaction, like with MongoDB with Panache
    }

    @Override
    public void afterCompletion(int status) {
        try (ClientSession closing = session) {
            if (status == Status.STATUS_COMMITTED) {
                closing.commitTransaction();
            } else {
                closing.abortTransaction();
            }
        }
    }
}
