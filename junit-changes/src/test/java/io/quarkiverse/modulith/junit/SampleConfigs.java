package io.quarkiverse.modulith.junit;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.config.Config;

import io.quarkiverse.modulith.junit.diff.TestConfigs;

/**
 * Configurations of the sample test classes run through the JUnit engine, counting how often they are loaded.
 */
final class SampleConfigs {

    static final AtomicInteger LOADS = new AtomicInteger();

    private SampleConfigs() {
    }

    static Config skipOptimizations() {
        LOADS.incrementAndGet();
        return TestConfigs.of(Map.of("quarkus.modulith.test.skip-optimizations", "true"));
    }

    static Config invalidOnNoChanges() {
        LOADS.incrementAndGet();
        return TestConfigs.of(Map.of("quarkus.modulith.test.on-no-changes", "never"));
    }
}
