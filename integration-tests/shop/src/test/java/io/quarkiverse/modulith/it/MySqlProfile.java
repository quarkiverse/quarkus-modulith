package io.quarkiverse.modulith.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class MySqlProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.datasource.db-kind", "mysql",
                "quarkus.datasource.devservices.image-name", "mysql:8.4.13",
                "quarkus.modulith.events.republish-outstanding-events-on-restart", "false");
    }
}
