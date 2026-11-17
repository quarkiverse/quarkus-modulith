package io.quarkiverse.modulith.deployment.fixtures.valid.order.internal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderRepository {

    private final List<String> orders = new CopyOnWriteArrayList<>();

    public void save(String orderId) {
        orders.add(orderId);
    }
}
