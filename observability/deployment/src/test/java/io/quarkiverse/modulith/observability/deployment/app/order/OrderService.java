package io.quarkiverse.modulith.observability.deployment.app.order;

import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import io.quarkiverse.modulith.observability.deployment.app.order.internal.OrderStore;

@ApplicationScoped
public class OrderService {

    @Inject
    Event<OrderCompleted> completed;

    @Inject
    Event<OrderCancelled> cancelled;

    @Inject
    OrderStore store;

    public void complete(String id, List<String> lines, int[] counts) {
        store.save(id);
        completed.fire(new OrderCompleted(id));
    }

    public CompletionStage<OrderCancelled> cancel(String id) {
        return cancelled.fireAsync(new OrderCancelled(id));
    }

    public void fail() {
        throw new IllegalStateException("broken");
    }
}
