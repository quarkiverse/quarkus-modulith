package io.quarkiverse.modulith.deployment.docs.fixtures.config;

import io.smallrye.config.WithDefault;

public interface ParentConfig {

    @WithDefault("10")
    int timeout();
}
