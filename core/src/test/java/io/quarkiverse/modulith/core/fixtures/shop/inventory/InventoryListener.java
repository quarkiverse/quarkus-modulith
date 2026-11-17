package io.quarkiverse.modulith.core.fixtures.shop.inventory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.core.fixtures.shop.order.OrderManagement;
import io.quarkiverse.modulith.core.fixtures.shop.order.OrderPlaced;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.vertx.web.Route;

@ApplicationScoped
public class InventoryListener {

    @Inject
    OrderManagement orders;

    @ApplicationModuleListener
    void on(OrderPlaced event) {
    }

    void observe(@Observes OrderPlaced event) {
    }

    void observeAsync(@ObservesAsync OrderPlaced event) {
    }

    @Incoming("orders")
    void consume(String message) {
    }

    @Scheduled(every = "10s")
    void tick() {
    }

    @Route(path = "/stock")
    void route() {
    }
}
