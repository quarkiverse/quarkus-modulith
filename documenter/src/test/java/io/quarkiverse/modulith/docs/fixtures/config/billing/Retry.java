package io.quarkiverse.modulith.docs.fixtures.config.billing;

import io.smallrye.config.WithDefault;

public interface Retry {

    @WithDefault("3")
    int attempts();
}
