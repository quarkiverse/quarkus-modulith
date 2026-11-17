package io.quarkiverse.modulith.events.jpa.deployment.routed;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.BeanManager;

import io.quarkiverse.modulith.events.jpa.deployment.fixtures.order.OrderPlaced;

@ApplicationScoped
public class RoutedObservers {

    public static final List<String> CALLS = new CopyOnWriteArrayList<>();

    void afterSuccess(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrderPlaced event) {
        CALLS.add("after-success:" + event.orderId());
    }

    @Outboxed
    void marked(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrderPlaced event) {
        CALLS.add("marked:" + event.orderId());
    }

    void qualified(@Observes(during = TransactionPhase.AFTER_SUCCESS) @Urgent OrderPlaced event) {
        CALLS.add("qualified:" + event.orderId());
    }

    void prioritized(@Observes(during = TransactionPhase.AFTER_SUCCESS) @Priority(10) OrderPlaced event) {
        CALLS.add("prioritized:" + event.orderId());
    }

    void afterCompletion(@Observes(during = TransactionPhase.AFTER_COMPLETION) OrderPlaced event) {
        CALLS.add("after-completion:" + event.orderId());
    }

    void inProgress(@Observes OrderPlaced event) {
        CALLS.add("in-progress:" + event.orderId());
    }

    void withBeanManager(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrderPlaced event, BeanManager beans) {
        CALLS.add("with-bean-manager:" + event.orderId());
    }
}
