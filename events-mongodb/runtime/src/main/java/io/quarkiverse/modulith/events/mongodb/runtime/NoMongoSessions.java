package io.quarkiverse.modulith.events.mongodb.runtime;

import java.util.Optional;

import jakarta.inject.Singleton;

import com.mongodb.client.ClientSession;

/**
 * Writes every publication on its own, used when MongoDB with Panache or JTA transactions are absent, or when
 * {@code quarkus.modulith.events.mongodb.transaction-management.enabled} is {@code false}.
 */
@Singleton
public class NoMongoSessions implements MongoSessions {

    @Override
    public Optional<ClientSession> current() {
        return Optional.empty();
    }
}
