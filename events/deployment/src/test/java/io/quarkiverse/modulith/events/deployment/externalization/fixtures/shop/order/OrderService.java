package io.quarkiverse.modulith.events.deployment.externalization.fixtures.shop.order;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OrderService {

    @Inject
    Event<OrderPlaced> placed;

    @Inject
    Event<OrderCancelled> cancelled;

    @Inject
    Event<OrderNoted> noted;

    public void place(OrderPlaced event) {
        placed.fire(event);
    }

    @Transactional
    public void placeInTransaction(OrderPlaced event) {
        placed.fire(event);
    }

    @Transactional
    public void placeAndRollBack(OrderPlaced event) {
        placed.fire(event);
        throw new IllegalStateException("rolled back on purpose");
    }

    public void cancel(OrderCancelled event) {
        cancelled.fire(event);
    }

    public void note(OrderNoted event) {
        noted.fire(event);
    }
}
