package io.quarkiverse.modulith.events.externalization.runtime;

import java.time.Duration;

record IntervalOutbox(Duration interval, Duration sendTimeout, Duration staleAfter,
        int batchSize) implements ExternalizationRuntimeConfig.Outbox {
}
