package io.quarkiverse.modulith.deployment.docs.fixtures.app.inventory;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.spi.BeanManager;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.deployment.docs.fixtures.app.order.OrderCancelled;
import io.quarkiverse.modulith.deployment.docs.fixtures.app.order.OrderCompleted;

public class InventoryListener {

    @ApplicationModuleListener
    void on(OrderCompleted event) {
    }

    @ApplicationModuleListener(async = false)
    void sync(OrderCancelled event) {
    }

    void observe(@Observes OrderCompleted event) {
    }

    void observeAsync(BeanManager beans, @ObservesAsync OrderCancelled event) {
    }

    void notAListener(String text) {
    }

    void none() {
    }
}
