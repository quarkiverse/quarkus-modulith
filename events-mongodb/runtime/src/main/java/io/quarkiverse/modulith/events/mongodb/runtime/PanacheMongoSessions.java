package io.quarkiverse.modulith.events.mongodb.runtime;

import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;

// Not a public API: MongoDB with Panache offers no other way to share its transaction's client session. Tested with
// Quarkus 3.40 and 4.0.0.CR1.
import io.quarkus.mongodb.panache.common.runtime.MongoOperations;

/**
 * Shares the client session MongoDB with Panache binds to the active JTA transaction (a {@code @Transactional}
 * method on a replica set). The session is looked up under Panache's key, and started under that key when the
 * publication is the first write of the transaction, so Panache entities written later in the transaction join it.
 * <p>
 * The session is started from the default MongoDB client, so the store needs the entities of the transaction to use
 * the default client too: a session of another client is refused by the driver.
 * <p>
 * In a transaction marked rollback-only, the bound session is still used, since its abort discards the writes. Without
 * a bound session none can be started (the transaction takes no more synchronizations), so writes are dropped.
 */
@Singleton
public class PanacheMongoSessions implements MongoSessions {

    private final TransactionSynchronizationRegistry registry;
    private final MongoClient client;

    @Inject
    public PanacheMongoSessions(TransactionSynchronizationRegistry registry, MongoClient client) {
        this.registry = registry;
        this.client = client;
    }

    @Override
    public Optional<ClientSession> current() {
        int status = registry.getTransactionStatus();
        if (status == Status.STATUS_MARKED_ROLLBACK) {
            return Optional.ofNullable(bound());
        }
        if (status != Status.STATUS_ACTIVE) {
            return Optional.empty();
        }
        ClientSession session = bound();
        if (Objects.isNull(session)) {
            session = client.startSession();
            session.startTransaction();
            registry.putResource(MongoOperations.SESSION_KEY, session);
            registry.registerInterposedSynchronization(new MongoSessionSynchronization(session));
        }
        return Optional.of(session);
    }

    @Override
    public boolean discardsWrites() {
        return registry.getTransactionStatus() == Status.STATUS_MARKED_ROLLBACK && Objects.isNull(bound());
    }

    private ClientSession bound() {
        return (ClientSession) registry.getResource(MongoOperations.SESSION_KEY);
    }
}
