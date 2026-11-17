package io.quarkiverse.modulith.events.jpa.deployment.rollback;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderPlaced;
import io.quarkus.narayana.jta.QuarkusTransaction;

/**
 * Marks its own transaction rollback-only without throwing, which also rolls back the completion written inside it.
 */
public class RollbackOnlyListener {

    @ApplicationModuleListener(id = "rollback-only")
    void on(OrderPlaced event) {
        QuarkusTransaction.setRollbackOnly();
    }
}
