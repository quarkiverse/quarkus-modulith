package io.quarkiverse.modulith.docs.fixtures.shop.shipping;

import org.springframework.stereotype.Service;

import io.quarkiverse.modulith.docs.fixtures.shop.inventory.Stock;

@Service
public class ShippingService {

    private final Stock stock;

    ShippingService(Stock stock) {
        this.stock = stock;
    }

    Stock stock() {
        return stock;
    }
}
