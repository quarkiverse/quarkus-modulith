package io.quarkiverse.modulith.core.fixtures.shop.order;

import jakarta.persistence.Entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class Order extends PanacheEntityBase {
    public static Order create() {
        return new Order();
    }
}
