package io.quarkiverse.modulith.events.mongodb.runtime;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import com.mongodb.client.MongoClient;

import io.quarkiverse.modulith.events.config.EventsFixedConfig;
import io.quarkiverse.modulith.events.core.EventSerializer;

/**
 * Creates the {@link MongoStore} from the default MongoDB client and the configuration.
 */
@Singleton
public class MongoStoreProducer {

    @Produces
    @Singleton
    MongoStore store(MongoClient client, EventSerializer serializer, MongoSessions sessions, MongoDbConfig config,
            EventsFixedConfig events) {
        return new MongoStore(client.getDatabase(config.database()), serializer, sessions,
                events.completionMode(), config.uuidRepresentation());
    }
}
