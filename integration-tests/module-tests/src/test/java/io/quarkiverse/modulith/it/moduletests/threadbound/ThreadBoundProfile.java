package io.quarkiverse.modulith.it.moduletests.threadbound;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class ThreadBoundProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.modulith.test.thread-bound-published-events", "true");
    }
}
