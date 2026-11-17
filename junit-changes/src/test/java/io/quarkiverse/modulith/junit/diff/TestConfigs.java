package io.quarkiverse.modulith.junit.diff;

import java.util.Map;

import org.eclipse.microprofile.config.Config;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Builds configurations from fixed properties only, without system properties or environment variables.
 */
public final class TestConfigs {

    private TestConfigs() {
    }

    public static Config of(Map<String, String> properties) {
        return new SmallRyeConfigBuilder()
                .withSources(new PropertiesConfigSource(properties, "test", 500))
                .build();
    }
}
