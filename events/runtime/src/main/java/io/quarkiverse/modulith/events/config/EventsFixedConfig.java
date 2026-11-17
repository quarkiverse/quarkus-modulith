package io.quarkiverse.modulith.events.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Registry settings fixed at build time because stores derive their mapping from them, for example the JPA store only
 * maps the archive entity in {@code archive} mode.
 */
@ConfigMapping(prefix = "quarkus.modulith.events")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface EventsFixedConfig {

    /**
     * What happens to a publication once its listener succeeded: {@code update} sets the completion date and keeps the
     * row, {@code delete} removes it, {@code archive} moves it to the archive.
     */
    @WithDefault("update")
    EventsConfig.CompletionMode completionMode();

    /**
     * When set, only CDI observers notified {@code AFTER_SUCCESS} whose method carries this annotation get event
     * publications, like Spring Modulith's {@code spring.modulith.events.registry-trigger-annotation}. By default
     * every such observer does, as every {@code @TransactionalEventListener} does in Spring Modulith.
     * {@code @ApplicationModuleListener} methods always get publications.
     */
    Optional<String> registryTriggerAnnotation();
}
