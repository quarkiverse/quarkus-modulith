package io.quarkiverse.modulith.core.fixtures.shop.catalog;

import jakarta.enterprise.inject.Produces;

import io.quarkiverse.modulith.core.fixtures.shop.catalog.internal.Stock;

public class Catalog {

    @Produces
    Stock stock;

    @Produces
    public Stock produce() {
        return new Stock();
    }
}
