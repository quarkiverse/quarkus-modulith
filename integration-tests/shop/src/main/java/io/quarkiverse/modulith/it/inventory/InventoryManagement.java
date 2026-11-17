package io.quarkiverse.modulith.it.inventory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import io.quarkiverse.modulith.it.inventory.internal.StockItem;

/**
 * The public API of the inventory module.
 */
@ApplicationScoped
public class InventoryManagement {

    @Inject
    EntityManager entityManager;

    @Transactional
    public int stockOf(String sku) {
        StockItem item = entityManager.find(StockItem.class, sku);
        return item == null ? 0 : item.quantity;
    }
}
