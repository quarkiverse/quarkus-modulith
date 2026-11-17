package io.quarkiverse.modulith.events.externalization.fakes;

import java.util.OptionalInt;

import io.quarkiverse.modulith.events.config.EventsConfig;

public record FixedResubmission(OptionalInt abandonAfterAttempts) implements EventsConfig.Resubmission {
}
