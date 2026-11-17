package io.quarkiverse.modulith.moments.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Passage of time events. These settings decide which beans the build registers, so changing them needs a rebuild.
 */
@ConfigMapping(prefix = "quarkus.modulith.moments")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface MomentsBuildTimeConfig {

    /**
     * Whether to register the {@code Moments} bean and its scheduler jobs. When {@code false}, no passage of time event
     * is published and neither {@code Moments}, {@code TimeMachine} nor {@code Now} can be injected. Spring Modulith:
     * {@code spring.modulith.moments.enabled}.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Whether the {@code Moments} bean is a {@code TimeMachine}, which shifts time and publishes every event on the way.
     * Meant for tests and local development, for example in the {@code test} profile:
     * {@code %test.quarkus.modulith.moments.enable-time-machine=true}. Spring Modulith:
     * {@code spring.modulith.moments.enable-time-machine}.
     */
    @WithDefault("false")
    boolean enableTimeMachine();
}
