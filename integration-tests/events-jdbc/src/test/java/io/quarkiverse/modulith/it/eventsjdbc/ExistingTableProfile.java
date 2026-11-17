package io.quarkiverse.modulith.it.eventsjdbc;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Starts the application on a database where the event publication table already exists, created by an init script
 * without indexes and with one incomplete publication.
 */
public class ExistingTableProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.datasource.devservices.init-script-path", "existing-table.sql");
    }
}
