package io.quarkiverse.modulith.it.eventsmongodb.repository;

import org.testcontainers.mongodb.MongoDBContainer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * One MongoDB replica set for the repository tests of the test JVM, on the image Quarkus Dev Services uses.
 */
public final class Mongo {

    private static MongoClient client;

    private Mongo() {
    }

    /**
     * @return the database with the given name, emptied
     */
    public static synchronized MongoDatabase emptyDatabase(String name) {
        if (client == null) {
            MongoDBContainer container = new MongoDBContainer("mongo:7.0.48");
            container.start();
            client = MongoClients.create(container.getReplicaSetUrl());
        }
        MongoDatabase database = client.getDatabase(name);
        database.drop();
        return database;
    }
}
