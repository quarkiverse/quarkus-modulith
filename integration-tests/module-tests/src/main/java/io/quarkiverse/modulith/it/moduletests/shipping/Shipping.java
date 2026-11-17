package io.quarkiverse.modulith.it.moduletests.shipping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.it.moduletests.inventory.Inventory;

@ApplicationScoped
public class Shipping {

    @Inject
    Inventory inventory;

    @Inject
    ShippingPolicy policy;

    public boolean canShip(String sku, int quantity) {
        return quantity <= policy.maxQuantity() && inventory.stockOf(sku) >= quantity;
    }
}
