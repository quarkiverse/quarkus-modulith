package io.quarkiverse.modulith.deployment.fixtures.valid.inventory.internal;

import jakarta.inject.Inject;
import jakarta.transaction.TransactionSynchronizationRegistry;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.deployment.fixtures.valid.order.OrderCompleted;

public class InventoryListener {

    @Inject
    Received received;

    @Inject
    TransactionSynchronizationRegistry transactions;

    @ApplicationModuleListener
    void on(OrderCompleted event) {
        received.add(event, transactions.getTransactionKey() != null);
    }
}
