package io.quarkiverse.modulith.events.jpa.deployment.fixtures.invoice.internal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Singleton;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderPlaced;

@Singleton
public class InvoiceListener {

    /**
     * Number of upcoming deliveries that throw.
     */
    public static final AtomicInteger FAILURES_TO_SIMULATE = new AtomicInteger();

    public static final List<OrderPlaced> INVOICED = new CopyOnWriteArrayList<>();

    @ApplicationModuleListener(id = "invoice.on-order-placed")
    void on(OrderPlaced event) {
        if (FAILURES_TO_SIMULATE.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
            throw new IllegalStateException("Simulated failure for " + event.orderId());
        }
        INVOICED.add(event);
    }
}
