package io.quarkiverse.modulith.it.inventory.internal;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.it.order.events.OrderPlaced;

public class InventoryListener {

    @Inject
    EntityManager entityManager;

    @ApplicationModuleListener(id = "inventory.reserve-stock")
    void on(OrderPlaced event) {
        StockItem item = Optional.ofNullable(entityManager.find(StockItem.class, event.sku()))
                .orElseThrow(() -> new IllegalStateException("Unknown sku " + event.sku()));
        item.quantity -= event.quantity();
    }
}
