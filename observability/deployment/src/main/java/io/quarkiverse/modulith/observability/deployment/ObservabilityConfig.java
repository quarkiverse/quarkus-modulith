package io.quarkiverse.modulith.observability.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Module observability: spans and timers of calls into modules, counters of module events.
 */
@ConfigMapping(prefix = "quarkus.modulith.observability")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface ObservabilityConfig {

    /**
     * Whether module observability is active. When off, the build adds no interceptor and no observer, so module
     * calls and events cost nothing extra. Fixed at build time.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Spans of module invocations. They need {@code quarkus-opentelemetry}.
     */
    Tracing tracing();

    /**
     * Timers of module invocations, event counters and publication gauges. They need {@code quarkus-micrometer}.
     */
    Metrics metrics();

    interface Tracing {

        /**
         * Whether calls into modules open OpenTelemetry spans. Fixed at build time.
         */
        @WithDefault("true")
        boolean enabled();
    }

    interface Metrics {

        /**
         * Whether module invocations are timed and module events counted with Micrometer. Fixed at build time.
         */
        @WithDefault("true")
        boolean enabled();
    }
}
