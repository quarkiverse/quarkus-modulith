package io.quarkiverse.modulith.runtime.events;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime configuration of the module listener dispatcher.
 */
@ConfigMapping(prefix = "quarkus.modulith.listeners")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ListenerConfig {

    /**
     * How long the application waits at shutdown for listeners that are still running, like Spring Modulith's
     * {@code spring.modulith.default-async-termination}. Deliveries that did not finish stay incomplete in the event
     * publication registry and are submitted again later.
     */
    @WithDefault("2s")
    Duration shutdownTimeout();

    /**
     * When a publication is marked completed: {@code in-listener}, inside the listener's transaction, or
     * {@code in-interceptor}, in a separate transaction after the listener's transaction committed.
     */
    @WithDefault("in-listener")
    CompletionTiming completionTiming();
}
