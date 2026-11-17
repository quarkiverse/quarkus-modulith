package io.quarkiverse.modulith.events.mongodb.runtime;

import org.bson.UuidRepresentation;

import com.mongodb.client.MongoDatabase;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.core.EventSerializer;

/**
 * Everything the MongoDB event publication repository works with.
 *
 * @param database the database of the publication collections
 * @param serializer turns events into JSON and back
 * @param sessions the client session of the current transaction, if any
 * @param completion what happens to completed publications
 * @param uuidRepresentation how identifiers are stored
 */
public record MongoStore(MongoDatabase database, EventSerializer serializer, MongoSessions sessions,
        CompletionMode completion, UuidRepresentation uuidRepresentation) {
}
