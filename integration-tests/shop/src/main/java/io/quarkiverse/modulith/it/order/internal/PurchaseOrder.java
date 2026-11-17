package io.quarkiverse.modulith.it.order.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class PurchaseOrder {

    @Id
    public String id;

    public String sku;

    public int quantity;
}
