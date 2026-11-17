package io.quarkiverse.modulith.events.jpa.deployment.fixtures.order;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OrderService {

    @Inject
    Event<OrderPlaced> events;

    @Transactional
    public void place(String orderId, int amount) {
        events.fire(new OrderPlaced(orderId, amount));
    }

    @Transactional
    public void placeAndRollBack(String orderId) {
        events.fire(new OrderPlaced(orderId, 1));
        throw new IllegalStateException("rolled back " + orderId);
    }
}
