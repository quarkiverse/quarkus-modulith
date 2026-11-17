package io.quarkiverse.modulith.core.fixtures.shop.order;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class OrderRepository implements PanacheRepositoryBase<LineItem, Long> {
}
