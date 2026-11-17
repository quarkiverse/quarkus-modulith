package io.quarkiverse.modulith.events.externalization.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Runtime settings of event externalization. Whether externalization is enabled and its mode are build-time settings
 * of the same prefix.
 */
@ConfigMapping(prefix = "quarkus.modulith.events.externalization")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface ExternalizationRuntimeConfig {

    /**
     * Whether the default externalization configuration sends events one at a time, so that the broker sees them in
     * the order they were published. Like Spring Modulith's
     * {@code spring.modulith.events.externalization.serialize-externalization}. An application that declares its own
     * {@code EventExternalizationConfiguration} decides this itself.
     */
    @WithDefault("false")
    boolean serializeExternalization();

    /**
     * When externalization is serialized, how long one send may wait for the broker's acknowledgement. A send that
     * takes longer fails, and the next event is sent, so that a broker that never answers cannot stop
     * externalization.
     */
    @WithDefault("30s")
    Duration serializedSendTimeout();

    /**
     * The outbox mode.
     */
    Outbox outbox();

    /**
     * Settings of the outbox mode, which records events as event publications and sends them from a job.
     */
    interface Outbox {

        /**
         * How often the job looks for publications to send, in addition to running after every transaction that
         * recorded events. Publications that failed are sent again on the next run.
         */
        @WithDefault("5s")
        Duration interval();

        /**
         * How long to wait for the broker to acknowledge a message before the publication counts as failed and the
         * run stops, so that later events do not overtake it.
         */
        @WithDefault("30s")
        Duration sendTimeout();

        /**
         * How long, on top of {@code send-timeout}, a publication may stay claimed or processing before another
         * instance takes it over, because the instance that claimed it stopped.
         */
        @WithDefault("1m")
        Duration staleAfter();

        /**
         * How many publications a run reads at once. Must be at least 1.
         */
        @WithDefault("100")
        int batchSize();
    }
}
