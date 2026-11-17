package io.quarkiverse.modulith.events.jdbc.runtime;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventSerializer;

/**
 * Everything a JDBC event publication repository works with.
 *
 * @param operations runs the statements
 * @param serializer turns events into the stored text and back
 * @param settings the database and the tables
 */
public record JdbcStore(JdbcOperations operations, EventSerializer serializer, JdbcRepositorySettings settings) {

    /**
     * Creates a store, detecting the database from a connection of the data source.
     */
    public static JdbcStore create(DataSource dataSource, EventSerializer serializer, Optional<String> schema,
            boolean legacyStructure, CompletionMode completion) {
        return create(dataSource, Optional.empty(), serializer, schema, legacyStructure, completion);
    }

    /**
     * Creates a store for the given database, or the one detected from a connection when it is not known.
     */
    public static JdbcStore create(DataSource dataSource, Optional<DatabaseType> type, EventSerializer serializer,
            Optional<String> schema, boolean legacyStructure, CompletionMode completion) {
        return new JdbcStore(new JdbcOperations(dataSource), serializer, new JdbcRepositorySettings(
                type.orElseGet(() -> detect(dataSource)), schema, legacyStructure, completion));
    }

    static DatabaseType detect(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return DatabaseType.fromProductName(connection.getMetaData().getDatabaseProductName());
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot detect the database of the event publication registry", e);
        }
    }
}
