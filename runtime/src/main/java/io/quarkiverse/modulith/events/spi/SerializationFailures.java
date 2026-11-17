package io.quarkiverse.modulith.events.spi;

import java.sql.SQLException;
import java.util.Objects;

/**
 * Recognizes a lost race on a claim. At REPEATABLE READ or SERIALIZABLE, PostgreSQL reports the losing concurrent
 * {@code UPDATE} with SQLState {@code 40001} instead of matching no row, and MySQL reports deadlocks the same way. A
 * claim that fails like this was simply won by another instance.
 * <p>
 * This is a helper for Quarkus Modulith extensions, not for applications.
 */
public final class SerializationFailures {

    static final String SERIALIZATION_FAILURE = "40001";

    private SerializationFailures() {
    }

    /**
     * @return whether the failure, or one of its causes, is an SQL serialization failure
     */
    public static boolean isSerializationFailure(Throwable failure) {
        for (Throwable current = failure; Objects.nonNull(current); current = current.getCause()) {
            if (current instanceof SQLException sql && SERIALIZATION_FAILURE.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
