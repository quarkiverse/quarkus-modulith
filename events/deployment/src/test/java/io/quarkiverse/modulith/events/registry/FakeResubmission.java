package io.quarkiverse.modulith.events.registry;

import java.util.OptionalInt;

import io.quarkiverse.modulith.events.config.EventsConfig;

record FakeResubmission(OptionalInt abandonAfterAttempts) implements EventsConfig.Resubmission {
}
