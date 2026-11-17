package io.quarkiverse.modulith.events.externalization.runtime;

import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;

/**
 * Publications whose claim fails in the database with the given SQLState.
 */
class ConflictingPublications extends ExternalizationPublications {

    private final String sqlState;

    ConflictingPublications(String sqlState) {
        this.sqlState = sqlState;
    }

    @Override
    public boolean claim(UUID publicationId, Duration staleAfter) {
        throw new IllegalStateException("claim failed", new SQLException("conflict", sqlState));
    }
}
