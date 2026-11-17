package io.quarkiverse.modulith.deployment.fixtures.valid.order;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.quarkiverse.modulith.deployment.fixtures.valid.order.internal.OrderRepository;

@ApplicationScoped
public class OrderManagement {

    @Inject
    Event<OrderCompleted> events;

    @Inject
    OrderRepository repository;

    @Transactional
    public void complete(String orderId) {
        repository.save(orderId);
        events.fire(new OrderCompleted(orderId));
    }

    @Transactional
    public void completeAndFail(String orderId) {
        repository.save(orderId);
        events.fire(new OrderCompleted(orderId));
        throw new IllegalStateException("rollback " + orderId);
    }

    public void completeWithoutTransaction(String orderId) {
        events.fire(new OrderCompleted(orderId));
    }
}
