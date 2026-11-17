package io.quarkiverse.modulith.events.jms.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime settings of the JMS externalization transport.
 */
@ConfigMapping(prefix = "quarkus.modulith.events.jms")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface JmsExternalizationConfig {

    /**
     * Whether the routing target of an externalized event names a queue or a topic. Applies to every event, like
     * Spring's {@code spring.jms.pub-sub-domain}, which Spring Modulith's JMS externalization follows through
     * {@code JmsTemplate}. Takes effect at startup.
     */
    @WithDefault("queue")
    JmsDestinationType destinationType();

    /**
     * How long to wait for the JMS provider to return from a send before the externalization counts as failed and the
     * event publication stays incomplete. The send runs on a worker thread that JMS cannot interrupt, so also set the
     * provider's own send timeout, such as `jms.sendTimeout` for Qpid JMS or `callTimeout` for Artemis: Qpid JMS
     * waits forever by default. A message the provider sends after the timeout may be sent again on resubmission. Takes
     * effect at startup.
     */
    @WithDefault("30s")
    Duration sendTimeout();
}
