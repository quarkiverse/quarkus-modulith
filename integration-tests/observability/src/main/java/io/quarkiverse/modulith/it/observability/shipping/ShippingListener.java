package io.quarkiverse.modulith.it.observability.shipping;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.it.observability.order.OrderPlaced;

/**
 * Ships placed orders through the event publication store. Orders with the id {@code lost} fail, so their publication
 * stays failed.
 */
@ApplicationScoped
public class ShippingListener {

    static final String LOST = "lost";

    @ApplicationModuleListener
    void on(OrderPlaced event) {
        if (LOST.equals(event.id())) {
            throw new IllegalStateException("Order " + event.id() + " cannot be shipped");
        }
    }
}
