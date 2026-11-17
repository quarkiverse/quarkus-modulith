package io.quarkiverse.modulith.events.jpa.deployment.readonlypublish;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderPlaced;

/**
 * Publishes an event from a read-only listener transaction, whose session never flushes by itself.
 */
@Singleton
public class AuditingListener {

    @Inject
    Event<OrderAudited> audits;

    @ApplicationModuleListener(id = "auditing", readOnlyTransaction = true)
    void on(OrderPlaced event) {
        audits.fire(new OrderAudited(event.orderId()));
    }
}
