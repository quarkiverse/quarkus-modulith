package io.quarkiverse.modulith.events.externalization.fakes;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkiverse.modulith.events.config.EventsConfig;

/**
 * The registry settings the outbox drain reads: only the attempt limit.
 */
public record FixedEventsConfig(OptionalInt abandonAfterAttempts) implements EventsConfig {

    @Override
    public boolean republishOutstandingEventsOnRestart() {
        return false;
    }

    @Override
    public Optional<Duration> republishInterval() {
        return Optional.empty();
    }

    @Override
    public Duration republishOlderThan() {
        return Duration.ofMinutes(5);
    }

    @Override
    public int republishBatchSize() {
        return 100;
    }

    @Override
    public Optional<Duration> deleteCompletedInterval() {
        return Optional.empty();
    }

    @Override
    public Duration deleteCompletedOlderThan() {
        return Duration.ofDays(7);
    }

    @Override
    public Staleness staleness() {
        throw new UnsupportedOperationException("the outbox drain has no staleness settings");
    }

    @Override
    public Resubmission resubmission() {
        return new FixedResubmission(abandonAfterAttempts);
    }
}
