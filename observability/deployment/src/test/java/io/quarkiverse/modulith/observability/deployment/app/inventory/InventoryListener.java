package io.quarkiverse.modulith.observability.deployment.app.inventory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.opentelemetry.api.trace.Tracer;
import io.quarkiverse.modulith.observability.deployment.app.order.OrderCompleted;

@ApplicationScoped
public class InventoryListener {

    @Inject
    Stock stock;

    @Inject
    Tracer tracer;

    void on(@Observes OrderCompleted event) {
        stock.reserve(event.id());
        tracer.spanBuilder("inner").startSpan().end();
    }
}
