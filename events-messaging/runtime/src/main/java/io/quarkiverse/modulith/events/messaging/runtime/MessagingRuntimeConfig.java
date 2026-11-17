package io.quarkiverse.modulith.events.messaging.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime settings of event externalization through Quarkus Messaging. The channels are build-time settings of the
 * same prefix.
 */
@ConfigMapping(prefix = "quarkus.modulith.events.messaging")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface MessagingRuntimeConfig {

    /**
     * How long to wait for the connector to acknowledge a message, that is, for the broker to confirm it, before the
     * externalization counts as failed and the event publication stays incomplete. A late acknowledgement is ignored,
     * so a resubmission may send the event twice. Takes effect at startup.
     */
    @WithDefault("30s")
    Duration ackTimeout();
}
