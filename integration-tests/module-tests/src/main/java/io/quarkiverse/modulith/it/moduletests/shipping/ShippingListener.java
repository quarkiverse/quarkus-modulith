package io.quarkiverse.modulith.it.moduletests.shipping;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.it.moduletests.inventory.StockReduced;

/**
 * Declares no scope, the extension makes it a singleton.
 */
public class ShippingListener {

    private final List<String> lowStock = new CopyOnWriteArrayList<>();

    @ApplicationModuleListener
    void on(StockReduced event) {
        if (event.left() < 10) {
            lowStock.add(event.sku());
        }
    }

    public List<String> lowStock() {
        return List.copyOf(lowStock);
    }
}
