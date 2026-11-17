package io.quarkiverse.modulith.docs.fixtures.shop.order;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import io.quarkiverse.modulith.docs.fixtures.shop.catalog.Catalog;
import io.quarkiverse.modulith.docs.fixtures.shop.order.internal.OrderRepository;

@Service
public class OrderManagement {

    private final ApplicationEventPublisher events;
    private final Catalog catalog;
    private final OrderRepository repository;

    OrderManagement(ApplicationEventPublisher events, Catalog catalog, OrderRepository repository) {
        this.events = events;
        this.catalog = catalog;
        this.repository = repository;
    }

    public void complete(Order order) {
        events.publishEvent(new OrderCompleted(order.id()));
    }

    public void cancel() {
        events.publishEvent(new OrderCancelled());
    }

    Catalog catalog() {
        return catalog;
    }

    OrderRepository repository() {
        return repository;
    }
}
