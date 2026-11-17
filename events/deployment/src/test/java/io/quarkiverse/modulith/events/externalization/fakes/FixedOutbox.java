package io.quarkiverse.modulith.events.externalization.fakes;

import java.time.Duration;

import io.quarkiverse.modulith.events.externalization.runtime.ExternalizationRuntimeConfig;

public record FixedOutbox(Duration sendTimeout, Duration staleAfter, int batchSize)
        implements
            ExternalizationRuntimeConfig.Outbox {

    @Override
    public Duration interval() {
        return Duration.ofHours(1);
    }
}
