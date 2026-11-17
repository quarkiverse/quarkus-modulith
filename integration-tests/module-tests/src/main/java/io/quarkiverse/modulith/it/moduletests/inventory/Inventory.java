package io.quarkiverse.modulith.it.moduletests.inventory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.it.moduletests.order.OrderPlaced;

@ApplicationScoped
public class Inventory {

    private static final int INITIAL_STOCK = 100;

    private final Map<String, Integer> stock = new ConcurrentHashMap<>();

    @Inject
    Event<StockReduced> reduced;

    public int stockOf(String sku) {
        return stock.getOrDefault(sku, INITIAL_STOCK);
    }

    @ApplicationModuleListener
    void on(OrderPlaced event) {
        int left = stock.merge(event.sku(), INITIAL_STOCK - event.quantity(), (old, ignored) -> old - event.quantity());
        reduced.fire(new StockReduced(event.sku(), left));
    }
}
