package io.quarkiverse.modulith.deployment.fixtures.model.init;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.modulith.ApplicationModuleInitializer;

@ApplicationScoped
public class RootInitializer implements ApplicationModuleInitializer {
    @Override
    public void initialize() {
        Calls.CALLS.add("root");
    }
}
