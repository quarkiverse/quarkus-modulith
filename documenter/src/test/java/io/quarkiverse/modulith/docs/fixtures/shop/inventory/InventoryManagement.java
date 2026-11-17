package io.quarkiverse.modulith.docs.fixtures.shop.inventory;

import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import io.quarkiverse.modulith.docs.fixtures.shop.order.OrderCancelled;
import io.quarkiverse.modulith.docs.fixtures.shop.order.OrderCompleted;

@Component
public class InventoryManagement {

    @ApplicationModuleListener
    void on(OrderCompleted event) {
    }

    @EventListener
    void on(OrderCancelled event) {
    }
}
