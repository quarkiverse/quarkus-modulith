package io.quarkiverse.modulith.core.par.nested.catalog;

import io.quarkiverse.modulith.core.par.nested.catalog.internal.Store;
import io.quarkiverse.modulith.core.par.nested.catalog.pricing.Pricing;

public class Catalog {
    public Pricing pricing() {
        return new Pricing(new Store());
    }
}
