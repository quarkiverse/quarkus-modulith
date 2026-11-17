package io.quarkiverse.modulith.events.jdbc.runtime;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

/**
 * Creates the event publication tables from the schema files of Spring Modulith, unless they exist. Each table is
 * checked through the JDBC metadata first, so an existing table, also one created by Flyway or Liquibase with another
 * layout, is left as it is.
 */
public final class DatabaseSchemaInitializer {

    private static final Logger LOG = Logger.getLogger(DatabaseSchemaInitializer.class);

    private final JdbcStore store;

    public DatabaseSchemaInitializer(JdbcStore store) {
        this.store = store;
    }

    /**
     * Creates the table, and the archive table in archive completion mode, when they do not exist.
     */
    public void initialize() {
        JdbcRepositorySettings settings = store.settings();
        DatabaseType type = settings.databaseType();
        try (Connection connection = store.operations().dataSource().getConnection()) {
            create(connection, settings.schema(), JdbcRepositorySettings.TABLE,
                    type.schemaResource(settings.legacyStructure()));
            if (settings.isArchive()) {
                create(connection, settings.schema(), JdbcRepositorySettings.ARCHIVE_TABLE,
                        type.archiveSchemaResource(settings.legacyStructure()));
            }
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Cannot create the event publication tables", e);
        }
    }

    static void create(Connection connection, Optional<String> configuredSchema, String table, String resource)
            throws SQLException, IOException {
        DatabaseMetaData metaData = connection.getMetaData();
        boolean upperCase = metaData.storesUpperCaseIdentifiers();
        boolean lowerCase = metaData.storesLowerCaseIdentifiers();
        Optional<String> schema = configuredSchema.map(name -> identifier(name, upperCase, lowerCase));
        if (exists(connection, schema, identifier(table, upperCase, lowerCase))) {
            LOG.debugf("Table %s exists, leaving it as it is", table);
            return;
        }
        List<String> statements = statements(read(resource));
        if (schema.isPresent()) {
            execute(connection, List.of("CREATE SCHEMA IF NOT EXISTS " + schema.get()));
            String initialSchema = connection.getSchema();
            connection.setSchema(schema.get());
            try {
                execute(connection, statements);
            } finally {
                connection.setSchema(initialSchema);
            }
        } else {
            execute(connection, statements);
        }
        LOG.infof("Created the event publication table %s from %s", table, resource);
    }

    static boolean exists(Connection connection, Optional<String> schema, String table) throws SQLException {
        String schemaPattern = schema.orElse(connection.getSchema());
        try (ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), schemaPattern, table,
                new String[] { "TABLE" })) {
            return tables.next();
        }
    }

    /**
     * @return the name as the database stores unquoted identifiers
     */
    static String identifier(String name, boolean upperCase, boolean lowerCase) {
        if (upperCase) {
            return name.toUpperCase(Locale.ROOT);
        }
        return lowerCase ? name.toLowerCase(Locale.ROOT) : name;
    }

    static List<String> statements(String script) {
        String withoutComments = script.lines()
                .filter(line -> !line.trim().startsWith("--"))
                .collect(Collectors.joining("\n"));
        return Arrays.stream(withoutComments.split(";"))
                .map(String::trim)
                .filter(statement -> !statement.isEmpty())
                .toList();
    }

    /**
     * @throws IOException if the resource is missing or cannot be read
     */
    static String read(String resource) throws IOException {
        InputStream found = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (Objects.isNull(found)) {
            throw new FileNotFoundException("Missing schema resource " + resource);
        }
        try (InputStream in = found) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void execute(Connection connection, List<String> statements) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
    }
}
