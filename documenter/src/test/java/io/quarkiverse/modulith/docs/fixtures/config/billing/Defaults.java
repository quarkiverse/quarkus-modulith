package io.quarkiverse.modulith.docs.fixtures.config.billing;

import io.smallrye.config.WithDefault;

public interface Defaults {

    @WithDefault("EUR")
    String currency();
}
