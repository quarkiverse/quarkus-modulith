package io.quarkiverse.modulith.it.notification.internal;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import io.quarkiverse.modulith.ApplicationModuleListener;
import io.quarkiverse.modulith.it.order.events.OrderAudited;

/**
 * A slow listener that counts its deliveries, so a test can see a publication delivered twice while the first
 * delivery is still running.
 */
public class AuditListener {

    public static final Map<String, AtomicInteger> DELIVERIES = new ConcurrentHashMap<>();
    /** Keeps each delivery running long enough for a second claim of the same publication to overlap it. */
    private static final Duration SLOW_DELIVERY = Duration.ofMillis(200);

    @ApplicationModuleListener(id = "notification.audit")
    void on(OrderAudited event) {
        DELIVERIES.computeIfAbsent(event.orderId(), id -> new AtomicInteger()).incrementAndGet();
        LockSupport.parkNanos(SLOW_DELIVERY.toNanos());
    }
}
