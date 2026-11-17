package io.quarkiverse.modulith.it.observability.order;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OrderService {

    @Inject
    Event<OrderPlaced> placed;

    @Transactional
    public String place(String id) {
        placed.fire(new OrderPlaced(id));
        return id;
    }
}
