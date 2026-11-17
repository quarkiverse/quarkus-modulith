package io.quarkiverse.modulith.events.jdbc.runtime;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Maps the current row of a result set, or skips it.
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * @return the mapped row, empty to leave the row out of the result
     */
    Optional<T> map(ResultSet resultSet) throws SQLException;
}
