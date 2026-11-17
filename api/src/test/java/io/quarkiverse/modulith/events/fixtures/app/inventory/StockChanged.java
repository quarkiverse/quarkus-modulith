package io.quarkiverse.modulith.events.fixtures.app.inventory;

import org.jmolecules.event.annotation.Externalized;

@Externalized("stock")
public record StockChanged(String sku) {
}
