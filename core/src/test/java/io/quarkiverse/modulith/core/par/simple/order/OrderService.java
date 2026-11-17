package io.quarkiverse.modulith.core.par.simple.order;

import io.quarkiverse.modulith.core.par.simple.inventory.Inventory;

public class OrderService {
    private final Inventory inventory = new Inventory();

    public int place(String sku) {
        return inventory.stock(sku);
    }
}
