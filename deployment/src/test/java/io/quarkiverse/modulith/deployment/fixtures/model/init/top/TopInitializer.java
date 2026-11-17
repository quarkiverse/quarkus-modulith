package io.quarkiverse.modulith.deployment.fixtures.model.init.top;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.modulith.ApplicationModuleInitializer;
import io.quarkiverse.modulith.deployment.fixtures.model.init.Calls;
import io.quarkiverse.modulith.deployment.fixtures.model.init.base.BaseInitializer;

@ApplicationScoped
public class TopInitializer implements ApplicationModuleInitializer {
    BaseInitializer base;

    @Override
    public void initialize() {
        Calls.CALLS.add("top");
    }
}
