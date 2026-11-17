package io.quarkiverse.modulith.it.order;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import io.quarkiverse.modulith.it.common.internal.Ids;
import io.quarkiverse.modulith.it.order.events.Address;
import io.quarkiverse.modulith.it.order.events.OrderPlaced;
import io.quarkiverse.modulith.it.order.events.OrderShipped;
import io.quarkiverse.modulith.it.order.internal.PurchaseOrder;

/**
 * The public API of the order module.
 */
@ApplicationScoped
public class OrderManagement {

    @Inject
    EntityManager entityManager;

    @Inject
    Event<OrderPlaced> events;

    @Inject
    Event<OrderShipped> shipped;

    @Transactional
    public String place(String sku, int quantity) {
        PurchaseOrder order = new PurchaseOrder();
        order.id = Ids.next("order");
        order.sku = sku;
        order.quantity = quantity;
        entityManager.persist(order);
        events.fire(new OrderPlaced(order.id, sku, quantity));
        return order.id;
    }

    @Transactional
    public void ship(String orderId, Address address, List<String> parcels) {
        shipped.fire(new OrderShipped(orderId, address, parcels));
    }
}
