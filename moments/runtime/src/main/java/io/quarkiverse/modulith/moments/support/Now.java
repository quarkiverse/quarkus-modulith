package io.quarkiverse.modulith.moments.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The current point in time and today. Inject it instead of calling {@code LocalDateTime.now()} so that a
 * {@link TimeMachine} can shift the time for tests and local development.
 */
public interface Now {

    /**
     * Returns the current point in time in the configured zone.
     *
     * @return never {@literal null}
     */
    LocalDateTime now();

    /**
     * Returns the current date in the configured zone.
     *
     * @return never {@literal null}
     */
    LocalDate today();

    /**
     * Returns the current instant.
     *
     * @return never {@literal null}
     */
    Instant instant();
}
