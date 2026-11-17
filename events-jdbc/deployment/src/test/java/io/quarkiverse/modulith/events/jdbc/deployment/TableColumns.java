package io.quarkiverse.modulith.events.jdbc.deployment;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import io.agroal.api.AgroalDataSource;

public final class TableColumns {

    private TableColumns() {
    }

    public static List<String> of(AgroalDataSource dataSource, String table) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                ResultSet result = connection.getMetaData().getColumns(null, "PUBLIC", table, null)) {
            List<String> columns = new ArrayList<>();
            while (result.next()) {
                columns.add(result.getString("COLUMN_NAME"));
            }
            return columns;
        }
    }
}
