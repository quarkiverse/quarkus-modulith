package io.quarkiverse.modulith.events.jdbc.runtime;

import java.util.Optional;

/**
 * The database of the registry's datasource as the build knows it from {@code db-kind}.
 *
 * @param type the database, empty for {@code db-kind=other}, where it is detected from the connection at startup
 */
public record JdbcDatabase(Optional<DatabaseType> type) {
}
