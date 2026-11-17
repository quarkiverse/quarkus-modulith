package io.quarkiverse.modulith.events.jdbc.deployment;

import java.util.Optional;

import io.quarkiverse.modulith.events.config.EventsConfig.CompletionMode;
import io.quarkiverse.modulith.events.config.EventsFixedConfig;

/**
 * Fixed registry settings for calling the build steps directly.
 */
public record FixedEventsConfig(CompletionMode completionMode, Optional<String> registryTriggerAnnotation)
        implements
            EventsFixedConfig {
}
