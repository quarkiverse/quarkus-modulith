package io.quarkiverse.modulith.events.registry;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkiverse.modulith.events.config.EventsConfig;

record FakeEventsConfig(boolean republishOutstandingEventsOnRestart, Optional<Duration> republishInterval,
        Duration republishOlderThan, int republishBatchSize, Optional<Duration> deleteCompletedInterval,
        Duration deleteCompletedOlderThan, EventsConfig.Staleness staleness,
        EventsConfig.Resubmission resubmission) implements EventsConfig {

    static FakeEventsConfig defaults() {
        return new FakeEventsConfig(false, Optional.empty(), Duration.ofMinutes(5), 2, Optional.empty(),
                Duration.ofDays(7), FakeStaleness.none(), new FakeResubmission(OptionalInt.empty()));
    }

    FakeEventsConfig withAbandonAfter(int attempts) {
        return new FakeEventsConfig(republishOutstandingEventsOnRestart, republishInterval, republishOlderThan,
                republishBatchSize, deleteCompletedInterval, deleteCompletedOlderThan, staleness,
                new FakeResubmission(OptionalInt.of(attempts)));
    }

    FakeEventsConfig withStaleness(FakeStaleness thresholds) {
        return new FakeEventsConfig(republishOutstandingEventsOnRestart, republishInterval, republishOlderThan,
                republishBatchSize, deleteCompletedInterval, deleteCompletedOlderThan, thresholds, resubmission);
    }

    FakeEventsConfig withBatchSize(int batchSize) {
        return new FakeEventsConfig(republishOutstandingEventsOnRestart, republishInterval, republishOlderThan,
                batchSize, deleteCompletedInterval, deleteCompletedOlderThan, staleness, resubmission);
    }

    FakeEventsConfig withJobs(boolean onRestart, Optional<Duration> republish, Optional<Duration> delete) {
        return new FakeEventsConfig(onRestart, republish, republishOlderThan, republishBatchSize, delete,
                deleteCompletedOlderThan, staleness, resubmission);
    }
}
