package io.quarkiverse.modulith.deployment.fixtures.model.init.top;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;

import io.quarkiverse.modulith.ApplicationModuleInitializer;
import io.quarkiverse.modulith.deployment.fixtures.model.init.Calls;

@Dependent
public class LateTopInitializer implements ApplicationModuleInitializer {

    @Override
    public void initialize() {
        Calls.CALLS.add("late top");
    }

    @PreDestroy
    void destroy() {
        Calls.CALLS.add("late top destroyed");
    }
}
