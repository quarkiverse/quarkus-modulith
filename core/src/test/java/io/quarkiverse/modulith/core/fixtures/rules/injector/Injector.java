package io.quarkiverse.modulith.core.fixtures.rules.injector;

import jakarta.inject.Inject;

@jakarta.enterprise.context.ApplicationScoped
public class Injector {
    @Inject
    io.quarkiverse.modulith.core.fixtures.rules.store.Store store;
}
