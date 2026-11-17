package io.quarkiverse.modulith.runtime.events;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Build-time configuration of the module listener dispatcher.
 */
@ConfigMapping(prefix = "quarkus.modulith.listeners")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ListenerBuildTimeConfig {

    /**
     * Where asynchronous listeners run: the Quarkus {@code worker} pool, or {@code virtual-threads}, which needs the
     * {@code quarkus-virtual-threads} extension.
     */
    @WithDefault("worker")
    ListenerExecutorKind executor();
}
