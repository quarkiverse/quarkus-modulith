package io.quarkiverse.modulith.it.eventsmongodb.repository;

import org.bson.UuidRepresentation;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;

public record MongoTarget(CompletionMode completion, UuidRepresentation uuidRepresentation) {
}
