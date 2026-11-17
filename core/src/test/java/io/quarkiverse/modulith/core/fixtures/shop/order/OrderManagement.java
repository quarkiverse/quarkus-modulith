package io.quarkiverse.modulith.core.fixtures.shop.order;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.core.fixtures.shop.catalog.api.Products;
import io.quarkiverse.modulith.core.fixtures.shop.kernel.Money;

@ApplicationScoped
public class OrderManagement {

    @Inject
    Event<OrderPlaced> events;

    final Products products;

    @Inject
    public OrderManagement(Products products) {
        this.products = products;
    }

    OrderManagement() {
        this(null);
    }

    public Money complete(long id) {
        Supplier<OrderPlaced> event = () -> OrderPlaced.of(id);
        events.fire(event.get());
        OrderPlaced.describe(id);
        return new Money(id);
    }
}
