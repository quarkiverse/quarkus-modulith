package io.quarkiverse.modulith.it.observability.inventory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkiverse.modulith.it.observability.order.OrderPlaced;

@ApplicationScoped
public class InventoryListener {

    void on(@Observes OrderPlaced event) {
    }
}
