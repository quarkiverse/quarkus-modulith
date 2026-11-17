package io.quarkiverse.modulith.it.flyway;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.agroal.api.AgroalDataSource;

/**
 * Shows what the migrations did, so the same tests run against the packaged and the native application.
 */
@Path("/migrations")
public class MigrationResource {

    @Inject
    AgroalDataSource dataSource;

    @GET
    @Path("/log")
    public List<String> log() throws SQLException {
        return query("SELECT entry FROM migration_log ORDER BY id");
    }

    @GET
    @Path("/history-tables")
    public List<String> historyTables() throws SQLException {
        return query("SELECT table_name FROM information_schema.tables "
                + "WHERE table_name LIKE 'flyway_schema_history%' ORDER BY table_name");
    }

    private List<String> query(String sql) throws SQLException {
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
