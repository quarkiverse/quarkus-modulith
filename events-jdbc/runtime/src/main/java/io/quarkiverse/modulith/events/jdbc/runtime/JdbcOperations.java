package io.quarkiverse.modulith.events.jdbc.runtime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

/**
 * Runs SQL statements on connections of a {@link DataSource}. Connections take part in the current JTA transaction
 * when there is one, which the Agroal data source takes care of.
 * <p>
 * Arguments are bound by type: an {@link Instant} as a timestamp, an {@link Optional} of an {@link Instant} as a
 * timestamp or SQL {@code NULL}, anything else with {@link PreparedStatement#setObject(int, Object)}. Timestamps go
 * through {@link Timestamp#from(Instant)}, in the JVM's default time zone, as Spring Modulith 2.1.2 binds them, so
 * rows it wrote read back the same. On columns without time zone (the HSQLDB, Oracle and SQL Server schemas) every
 * instance must therefore run in the same time zone.
 */
public final class JdbcOperations {

    private final DataSource dataSource;

    public JdbcOperations(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public DataSource dataSource() {
        return dataSource;
    }

    /**
     * @return the number of rows the statement changed
     */
    public int update(String sql, Object... arguments) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, arguments);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw failure(sql, e);
        }
    }

    /**
     * @return the rows the mapper kept, in the order of the result set
     */
    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... arguments) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, arguments);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (resultSet.next()) {
                    mapper.map(resultSet).ifPresent(result::add);
                }
                return result;
            }
        } catch (SQLException e) {
            throw failure(sql, e);
        }
    }

    /**
     * @return the number in the first column of the first row of a {@code SELECT COUNT(*)} statement
     */
    public int count(String sql, Object... arguments) {
        return query(sql, resultSet -> Optional.of(resultSet.getInt(1)), arguments).get(0);
    }

    static void bind(PreparedStatement statement, Object... arguments) throws SQLException {
        for (int i = 0; i < arguments.length; i++) {
            Object argument = arguments[i];
            if (argument instanceof Instant instant) {
                statement.setTimestamp(i + 1, Timestamp.from(instant));
            } else if (argument instanceof Optional<?> optional) {
                bindOptional(statement, i + 1, optional);
            } else {
                statement.setObject(i + 1, argument);
            }
        }
    }

    private static void bindOptional(PreparedStatement statement, int index, Optional<?> optional)
            throws SQLException {
        if (optional.isEmpty()) {
            statement.setNull(index, Types.TIMESTAMP);
        } else if (optional.get() instanceof Instant instant) {
            statement.setTimestamp(index, Timestamp.from(instant));
        } else {
            throw new IllegalArgumentException("Only an Optional of an Instant can be bound, not one of "
                    + optional.get().getClass().getName());
        }
    }

    static IllegalStateException failure(String sql, SQLException e) {
        return new IllegalStateException("Event publication statement failed: " + sql, e);
    }
}
