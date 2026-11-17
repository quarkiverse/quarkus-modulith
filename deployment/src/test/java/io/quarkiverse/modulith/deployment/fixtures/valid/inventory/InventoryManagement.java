package io.quarkiverse.modulith.deployment.fixtures.valid.inventory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.deployment.fixtures.valid.inventory.internal.Received;

@ApplicationScoped
public class InventoryManagement {

    @Inject
    Received received;

    public int updates() {
        return received.events().size();
    }
}
