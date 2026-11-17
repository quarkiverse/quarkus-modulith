package io.quarkiverse.modulith.flyway.deployment;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.agroal.api.AgroalDataSource;

public final class MigrationLog {

    private MigrationLog() {
    }

    /**
     * @return the entries of the migration log, in the order the migrations wrote them
     */
    public static List<String> entries(AgroalDataSource dataSource) throws SQLException {
        return query(dataSource, "SELECT ENTRY FROM MIGRATION_LOG ORDER BY ID");
    }

    /**
     * @return the lower-case names of the Flyway history tables, sorted
     */
    public static List<String> historyTables(AgroalDataSource dataSource) throws SQLException {
        return query(dataSource, "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE LOWER(TABLE_NAME) LIKE 'flyway_schema_history%' ORDER BY TABLE_NAME").stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .toList();
    }

    private static List<String> query(AgroalDataSource dataSource, String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            List<String> values = new ArrayList<>();
            while (result.next()) {
                values.add(result.getString(1));
            }
            return values;
        }
    }
}
