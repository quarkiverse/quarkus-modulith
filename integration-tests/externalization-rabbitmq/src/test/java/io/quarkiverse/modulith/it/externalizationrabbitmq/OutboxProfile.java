package io.quarkiverse.modulith.it.externalizationrabbitmq;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class OutboxProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.modulith.events.externalization.mode", "outbox");
    }
}
