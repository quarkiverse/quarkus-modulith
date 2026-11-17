package io.quarkiverse.modulith.observability.runtime;

import java.time.Duration;

/**
 * Publication gauge settings with explicit values.
 */
record GaugeSettings(boolean enabled, Duration refreshInterval, boolean includeCompleted)
        implements
            PublicationGaugesConfig {
}
