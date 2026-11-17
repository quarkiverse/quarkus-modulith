package io.quarkiverse.modulith.deployment.docs.fixtures.app.order;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.deployment.docs.fixtures.app.catalog.Catalog;
import io.quarkiverse.modulith.deployment.docs.fixtures.app.catalog.spi.Pricing;
import io.quarkiverse.modulith.deployment.docs.fixtures.app.order.internal.InternalEvent;

@ApplicationScoped
public class OrderService {

    @Inject
    Catalog catalog;

    @Inject
    Instance<Pricing> pricing;

    @Inject
    Event<OrderCompleted> completed;

    @Inject
    Event<OrderCancelled> cancelled;

    @Inject
    Event<InternalEvent> internal;

    String plain;

    OrderService() {
        this("plain");
    }

    OrderService(String plain) {
        this.plain = plain;
    }

    public void complete() {
        String[] ids = new String[] { "1" };
        completed.fire(new OrderCompleted(ids[0]));
    }

    public void cancel(String reason) {
        Runnable fire = () -> cancelled.fire(new OrderCancelled());
        fire.run();
    }
}
