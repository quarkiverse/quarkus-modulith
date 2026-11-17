package io.quarkiverse.modulith.events.registry;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import io.quarkiverse.modulith.events.EventPublication.Status;
import io.quarkiverse.modulith.events.tck.InMemoryEventPublicationRepository;

/**
 * Fails claims the way a database does when another instance wins the race, or with another error.
 */
final class ConflictingRepository extends InMemoryEventPublicationRepository {

    private final String sqlState;

    ConflictingRepository(String sqlState) {
        this.sqlState = sqlState;
    }

    @Override
    public boolean claim(UUID identifier, Set<Status> eligible, Instant staleBefore, Instant claimDate) {
        throw new IllegalStateException("claim failed", new SQLException("conflict", sqlState));
    }
}
