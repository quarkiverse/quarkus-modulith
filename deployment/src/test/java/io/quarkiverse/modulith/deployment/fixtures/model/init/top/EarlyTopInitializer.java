package io.quarkiverse.modulith.deployment.fixtures.model.init.top;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.modulith.ApplicationModuleInitializer;
import io.quarkiverse.modulith.deployment.fixtures.model.init.Calls;

@ApplicationScoped
@Priority(-1)
public class EarlyTopInitializer implements ApplicationModuleInitializer {
    @Override
    public void initialize() {
        Calls.CALLS.add("early top");
    }
}
