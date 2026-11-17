package io.quarkiverse.modulith.events.mongodb.runtime;

import java.util.Optional;

import com.mongodb.client.ClientSession;

/**
 * Gives the client session of the current transaction, so publications are written in the same MongoDB transaction
 * as the business data.
 */
public interface MongoSessions {

    /**
     * @return the session to write with, empty to write outside a session
     */
    Optional<ClientSession> current();

    /**
     * @return whether writes are dropped: the current transaction can only roll back and no session joins it, so a
     *         write outside a session would outlive the rollback
     */
    default boolean discardsWrites() {
        return false;
    }
}
