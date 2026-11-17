package io.quarkiverse.modulith.it.eventsjdbc.repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.sql.DataSource;

public final class Tables {

    public static final List<String> NAMES = List.of("EVENT_PUBLICATION", "EVENT_PUBLICATION_ARCHIVE");

    private Tables() {
    }

    /**
     * Drops both tables in the schema, or in the connection's schema.
     */
    public static void drop(DataSource dataSource, Optional<String> schema) {
        for (String table : NAMES) {
            if (exists(dataSource, schema, table)) {
                execute(dataSource, "DROP TABLE " + schema.map(name -> name + ".").orElse("") + table);
            }
        }
    }

    /**
     * Deletes the rows of both tables, where they exist.
     */
    public static void clear(DataSource dataSource, Optional<String> schema) {
        for (String table : NAMES) {
            if (exists(dataSource, schema, table)) {
                execute(dataSource, "DELETE FROM " + schema.map(name -> name + ".").orElse("") + table);
            }
        }
    }

    public static boolean exists(DataSource dataSource, Optional<String> schema, String table) {
        return !columns(dataSource, schema, table).isEmpty();
    }

    /**
     * @return the upper-case column names of the table, empty if it does not exist
     */
    public static List<String> columns(DataSource dataSource, Optional<String> schema, String table) {
        try (Connection connection = dataSource.getConnection()) {
            List<String> columns = new ArrayList<>();
            for (String name : List.of(table, table.toLowerCase(Locale.ROOT))) {
                String schemaPattern = schema.map(value -> caseOf(name, value)).orElse(connection.getSchema());
                try (ResultSet result = connection.getMetaData().getColumns(connection.getCatalog(), schemaPattern, name,
                        null)) {
                    while (result.next()) {
                        columns.add(result.getString("COLUMN_NAME").toUpperCase(Locale.ROOT));
                    }
                }
            }
            return columns.stream().distinct().toList();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void execute(DataSource dataSource, String sql) {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String caseOf(String table, String schema) {
        return table.equals(table.toUpperCase(Locale.ROOT)) ? schema.toUpperCase(Locale.ROOT)
                : schema.toLowerCase(Locale.ROOT);
    }
}
