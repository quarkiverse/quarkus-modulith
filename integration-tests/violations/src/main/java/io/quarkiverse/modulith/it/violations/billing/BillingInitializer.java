package io.quarkiverse.modulith.it.violations.billing;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.ApplicationModuleInitializer;
import io.quarkiverse.modulith.it.violations.Initializations;

@ApplicationScoped
public class BillingInitializer implements ApplicationModuleInitializer {

    private final Initializations initializations;

    @Inject
    public BillingInitializer(Initializations initializations) {
        this.initializations = initializations;
    }

    @Override
    public void initialize() {
        initializations.ran("billing");
    }
}
