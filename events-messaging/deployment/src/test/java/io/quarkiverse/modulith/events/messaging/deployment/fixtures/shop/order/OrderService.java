package io.quarkiverse.modulith.events.messaging.deployment.fixtures.shop.order;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderService {

    @Inject
    Event<OrderPlaced> placed;

    @Inject
    Event<OrderShipped> shipped;

    @Inject
    Event<OrderCancelled> cancelled;

    @Inject
    Event<OrderRerouted> rerouted;

    @Inject
    Event<OrderAudited> audited;

    public void place(OrderPlaced event) {
        placed.fire(event);
    }

    public void ship(OrderShipped event) {
        shipped.fire(event);
    }

    public void cancel(OrderCancelled event) {
        cancelled.fire(event);
    }

    public void reroute(OrderRerouted event) {
        rerouted.fire(event);
    }

    public void audit(OrderAudited event) {
        audited.fire(event);
    }
}
