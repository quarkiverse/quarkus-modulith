package io.quarkiverse.modulith.docs.fixtures.shop.inventory.internal;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

import io.quarkiverse.modulith.docs.fixtures.shop.inventory.Stock;

@Component
@SecondaryAdapter
public class StockAdapter implements Stock {

    @Override
    public int available(String product) {
        return 0;
    }
}
