package io.quarkiverse.modulith.it.moments;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Runs the application with ISO weeks ({@code de-DE}) instead of the {@code en-US} weeks of its configuration.
 */
public class GermanWeeks implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("quarkus.modulith.moments.locale", "de-DE");
    }
}
