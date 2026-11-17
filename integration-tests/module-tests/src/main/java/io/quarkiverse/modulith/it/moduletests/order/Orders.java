package io.quarkiverse.modulith.it.moduletests.order;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.it.moduletests.catalog.Catalog;
import io.quarkiverse.modulith.it.moduletests.shared.Sequence;

@ApplicationScoped
public class Orders {

    @Inject
    Catalog catalog;

    @Inject
    Sequence sequence;

    @Inject
    Event<OrderPlaced> placed;

    /**
     * @return the identifier of the new order
     */
    public String place(String sku, int quantity) {
        if (!catalog.exists(sku)) {
            throw new IllegalArgumentException("Unknown product " + sku);
        }
        String orderId = "o-" + sequence.next();
        placed.fire(new OrderPlaced(orderId, sku, quantity));
        return orderId;
    }
}
