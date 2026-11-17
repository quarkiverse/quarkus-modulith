package io.quarkiverse.modulith.observability.runtime;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * The {@code modulith.event.publications} gauges, which count the event publications per status.
 */
@ConfigMapping(prefix = "quarkus.modulith.observability.metrics.publications")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface PublicationGaugesConfig {

    /**
     * Whether to register the gauges. When {@code false}, no gauge is registered and the store is never counted.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * How often the gauges count the publications again, each time in a transaction of their own. Scrapes read the
     * last counts and never query the store. Must be positive; turn the gauges off with {@code enabled} instead.
     */
    @WithDefault("30S")
    Duration refreshInterval();

    /**
     * Whether to count the completed publications too. In the {@code update} completion mode they are kept and grow
     * with every event, so counting them scans an ever larger table and is off by default.
     */
    @WithDefault("false")
    boolean includeCompleted();
}
