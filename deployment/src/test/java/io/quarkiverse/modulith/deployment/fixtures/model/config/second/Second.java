package io.quarkiverse.modulith.deployment.fixtures.model.config.second;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.deployment.fixtures.model.config.first.First;

@ApplicationScoped
public class Second {
    @Inject
    First first;
}
