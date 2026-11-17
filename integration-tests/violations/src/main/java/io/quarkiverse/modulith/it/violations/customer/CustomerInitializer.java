package io.quarkiverse.modulith.it.violations.customer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.ApplicationModuleInitializer;
import io.quarkiverse.modulith.it.violations.Initializations;

@ApplicationScoped
public class CustomerInitializer implements ApplicationModuleInitializer {

    private final Initializations initializations;

    @Inject
    public CustomerInitializer(Initializations initializations) {
        this.initializations = initializations;
    }

    @Override
    public void initialize() {
        initializations.ran("customer");
    }
}
