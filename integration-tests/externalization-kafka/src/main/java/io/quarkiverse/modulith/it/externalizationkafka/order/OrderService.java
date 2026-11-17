package io.quarkiverse.modulith.it.externalizationkafka.order;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OrderService {

    @Inject
    Event<OrderPlaced> placed;

    @Inject
    Event<OrderShipped> shipped;

    @Inject
    Event<OrderRerouted> rerouted;

    @Transactional
    public void place(OrderPlaced event) {
        placed.fire(event);
    }

    @Transactional
    public void ship(OrderShipped event) {
        shipped.fire(event);
    }

    @Transactional
    public void reroute(OrderRerouted event) {
        rerouted.fire(event);
    }
}
