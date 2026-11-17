package io.quarkiverse.modulith.deployment.fixtures.model.init.base;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.modulith.ApplicationModuleInitializer;
import io.quarkiverse.modulith.deployment.fixtures.model.init.Calls;

@ApplicationScoped
public class BaseInitializer implements ApplicationModuleInitializer {
    @Override
    public void initialize() {
        Calls.CALLS.add("base");
    }
}
