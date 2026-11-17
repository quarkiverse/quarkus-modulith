package io.quarkiverse.modulith.deployment.docs.fixtures.app.inventory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.deployment.docs.fixtures.app.catalog.Catalog;
import io.quarkiverse.modulith.deployment.docs.fixtures.app.order.OrderService;

@ApplicationScoped
public class Stock {

    final OrderService orders;

    Catalog catalog;

    Stock(OrderService orders) {
        this.orders = orders;
    }

    @Inject
    void catalog(Catalog catalog) {
        this.catalog = catalog;
    }
}
