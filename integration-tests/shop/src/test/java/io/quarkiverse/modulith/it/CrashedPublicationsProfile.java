package io.quarkiverse.modulith.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Starts the application on a table a crashed instance left behind. The processing threshold lets the new instance
 * take over the delivery the crashed one was running.
 */
public class CrashedPublicationsProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.hibernate-orm.sql-load-script", "crashed-publications.sql",
                "quarkus.modulith.events.staleness.processing", "10m");
    }
}
