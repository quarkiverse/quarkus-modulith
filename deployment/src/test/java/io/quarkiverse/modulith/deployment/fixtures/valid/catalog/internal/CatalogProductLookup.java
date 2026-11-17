package io.quarkiverse.modulith.deployment.fixtures.valid.catalog.internal;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.modulith.deployment.fixtures.valid.catalog.spi.ProductLookup;
import io.quarkiverse.modulith.deployment.fixtures.valid.shared.util.Money;

@ApplicationScoped
public class CatalogProductLookup implements ProductLookup {

    @Override
    public String name(String sku) {
        return sku + " " + new Money(10);
    }
}
