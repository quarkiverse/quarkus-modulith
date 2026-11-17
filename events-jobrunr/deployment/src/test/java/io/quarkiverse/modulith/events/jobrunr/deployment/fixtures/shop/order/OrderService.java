package io.quarkiverse.modulith.events.jobrunr.deployment.fixtures.shop.order;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OrderService {

    @Inject
    Event<OrderPlaced> placed;

    @Inject
    Event<OrderNoted> noted;

    @Transactional
    public void place(OrderPlaced event) {
        placed.fire(event);
    }

    @Transactional
    public void placeAndRollBack(OrderPlaced event) {
        placed.fire(event);
        throw new IllegalStateException("rolled back on purpose");
    }

    public void placeWithoutTransaction(OrderPlaced event) {
        placed.fire(event);
    }

    @Transactional
    public void note(OrderNoted event) {
        noted.fire(event);
    }
}
