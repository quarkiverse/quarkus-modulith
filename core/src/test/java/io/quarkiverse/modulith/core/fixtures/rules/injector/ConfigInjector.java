package io.quarkiverse.modulith.core.fixtures.rules.injector;

import jakarta.inject.Inject;

@org.springframework.context.annotation.Configuration
public class ConfigInjector {
    @Inject
    io.quarkiverse.modulith.core.fixtures.rules.store.Store store;
}
