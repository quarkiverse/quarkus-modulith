package io.quarkiverse.modulith.it.externalizationjms.order;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OrderService {

    @Inject
    Event<OrderPlaced> placed;

    @Transactional
    public void place(OrderPlaced order) {
        placed.fire(order);
    }

    @Transactional
    public void placeAndRollBack(OrderPlaced order) {
        placed.fire(order);
        throw new IllegalStateException("rolled back on purpose");
    }
}
