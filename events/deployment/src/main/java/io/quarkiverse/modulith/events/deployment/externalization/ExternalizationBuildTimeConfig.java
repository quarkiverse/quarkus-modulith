package io.quarkiverse.modulith.events.deployment.externalization;

import io.quarkiverse.modulith.events.ExternalizationMode;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build-time settings of event externalization. They decide which beans and observers the build registers.
 */
@ConfigMapping(prefix = "quarkus.modulith.events.externalization")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ExternalizationBuildTimeConfig {

    /**
     * Whether events are externalized when a transport is present, for example {@code quarkus-modulith-events-messaging}
     * or an {@code EventExternalizationTransport} bean of the application. Like Spring Modulith's
     * {@code spring.modulith.events.externalization.enabled}.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * How events reach the broker: {@code module-listener} sends them after the publishing transaction commits,
     * {@code outbox} records them in the publishing transaction and sends them from a job, through the event
     * publication registry or through JobRunr when {@code quarkus-modulith-events-jobrunr} is present. Like Spring
     * Modulith's {@code spring.modulith.events.externalization.mode}.
     */
    @WithDefault("module-listener")
    ExternalizationMode mode();
}
