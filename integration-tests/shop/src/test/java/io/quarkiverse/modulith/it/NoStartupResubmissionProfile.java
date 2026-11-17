package io.quarkiverse.modulith.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Keeps the startup resubmission from racing the registries under test for the same rows.
 */
public class NoStartupResubmissionProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.modulith.events.republish-outstanding-events-on-restart", "false");
    }
}
