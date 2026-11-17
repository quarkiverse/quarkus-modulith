package io.quarkiverse.modulith.it.inventory.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class StockItem {

    @Id
    public String sku;

    public int quantity;
}
