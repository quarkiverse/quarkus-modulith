package io.quarkiverse.modulith.testing;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime settings of the test support in {@code quarkus-modulith-junit}. The build-time settings of module tests
 * (module, bootstrap mode, extra modules) share the prefix but live in the deployment module.
 * <p>
 * This package is a documented SPI for {@code quarkus-modulith-junit}, not for applications.
 */
@ConfigMapping(prefix = "quarkus.modulith.test")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ModuleTestRuntimeConfig {

    /**
     * Whether the {@code PublishedEvents} of a test only contain the events published by the test's thread and the
     * threads it starts, instead of all events published while the test runs. Turn it on when tests run in parallel.
     */
    @WithDefault("false")
    boolean threadBoundPublishedEvents();
}
