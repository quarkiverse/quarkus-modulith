package io.quarkiverse.modulith.it.inventory.internal;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import io.quarkus.runtime.StartupEvent;

@Singleton
public class StockInitializer {

    @Inject
    EntityManager entityManager;

    @Transactional
    void onStart(@Observes StartupEvent event) {
        if (entityManager.find(StockItem.class, "sku-1") == null) {
            StockItem item = new StockItem();
            item.sku = "sku-1";
            item.quantity = 1000;
            entityManager.persist(item);
        }
    }
}
