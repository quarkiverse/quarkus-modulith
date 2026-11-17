package io.quarkiverse.modulith.it.springcompat.common.time;

import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * An internal type of the open common module, which every module may use.
 */
@ApplicationScoped
public class Clock {

    public Instant now() {
        return Instant.EPOCH;
    }
}
