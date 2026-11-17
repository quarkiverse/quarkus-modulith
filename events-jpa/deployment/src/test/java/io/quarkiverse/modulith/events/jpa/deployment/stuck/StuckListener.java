package io.quarkiverse.modulith.events.jpa.deployment.stuck;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderPlaced;

/**
 * Still running when the application stops, and outlasts the shutdown timeout.
 */
public class StuckListener {

    public static final List<String> STARTED = new CopyOnWriteArrayList<>();
    public static final List<String> FINISHED = new CopyOnWriteArrayList<>();
    static final CountDownLatch NEVER = new CountDownLatch(1);

    @ApplicationModuleListener(id = "stuck")
    void on(OrderPlaced event) throws InterruptedException {
        STARTED.add(event.orderId());
        boolean released = NEVER.await(5, TimeUnit.SECONDS);
        FINISHED.add(event.orderId() + (released ? ":released" : ":timed-out"));
    }
}
