package io.quarkiverse.modulith.observability.deployment;

import jakarta.inject.Singleton;

import io.micrometer.core.instrument.Counter;
import io.opentelemetry.api.trace.SpanBuilder;
import io.quarkiverse.modulith.observability.ModuleInvocation;
import io.quarkiverse.modulith.observability.ModulithEventMetrics;
import io.quarkiverse.modulith.observability.ModulithEventMetricsCustomizer;
import io.quarkiverse.modulith.observability.ModulithSpanCustomizer;
import io.quarkiverse.modulith.observability.deployment.app.order.OrderCancelled;
import io.quarkiverse.modulith.observability.deployment.app.order.OrderCompleted;

/**
 * Customizes the spans and event counters of the test application.
 */
@Singleton
public class TestCustomizers implements ModulithSpanCustomizer, ModulithEventMetricsCustomizer {

    @Override
    public void customize(ModuleInvocation invocation, SpanBuilder span) {
        span.setAttribute("listener", invocation.isEventListener());
    }

    @Override
    public void customize(ModulithEventMetrics metrics) {
        metrics.customize(Object.class, (event, builder) -> builder.tag("all", "yes"))
                .customize(OrderCompleted.class, (event, builder) -> builder.tag("id", event.id()))
                .customize(OrderCancelled.class, event -> Counter.builder("orders.cancelled"))
                .customize(String.class, event -> Counter.builder("never"));
    }
}
