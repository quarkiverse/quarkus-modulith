package io.quarkiverse.modulith.events.jdbc.runtime;

import java.util.Optional;
import java.util.regex.Pattern;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;

/**
 * How the JDBC repository reaches its tables.
 *
 * @param databaseType the database, detected from the connection
 * @param schema the schema of the tables, empty for the connection's default schema
 * @param legacyStructure whether the tables have the version 1 structure (no status, attempts or resubmission date)
 * @param completion what happens to completed publications
 */
public record JdbcRepositorySettings(DatabaseType databaseType, Optional<String> schema, boolean legacyStructure,
        CompletionMode completion) {

    static final String TABLE = "EVENT_PUBLICATION";
    static final String ARCHIVE_TABLE = "EVENT_PUBLICATION_ARCHIVE";

    /**
     * An unquoted SQL identifier. The schema name goes into the statement text, so nothing else is accepted.
     */
    static final Pattern SCHEMA_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,62}");

    public JdbcRepositorySettings {
        if (schema.filter(name -> !SCHEMA_NAME.matcher(name).matches()).isPresent()) {
            throw new IllegalStateException("quarkus.modulith.events.jdbc.schema must be an unquoted SQL identifier "
                    + "(a letter or underscore, then up to 62 letters, digits or underscores), but is: " + schema.get());
        }
        if (schema.isPresent() && !databaseType.isSchemaSupported()) {
            throw new IllegalStateException("quarkus.modulith.events.jdbc.schema is not supported on "
                    + databaseType.name() + ". Remove the property, or use H2, HSQLDB or PostgreSQL.");
        }
    }

    /**
     * @return the table publications are written to, qualified with the schema if one is set
     */
    public String table() {
        return qualified(TABLE);
    }

    /**
     * @return the archive table, qualified with the schema if one is set
     */
    public String archiveTable() {
        return qualified(ARCHIVE_TABLE);
    }

    /**
     * @return the table completed publications are read from: the archive table in archive mode, the table otherwise
     */
    public String completedTable() {
        return isArchive() ? archiveTable() : table();
    }

    public boolean isArchive() {
        return completion == CompletionMode.ARCHIVE;
    }

    private String qualified(String table) {
        return schema.map(name -> name + "." + table).orElse(table);
    }
}
