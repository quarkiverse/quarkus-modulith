package io.quarkiverse.modulith.observability.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkiverse.modulith.observability.deployment.app.ObservedApplication;
import io.quarkiverse.modulith.observability.deployment.app.order.OrderService;
import io.quarkus.test.QuarkusExtensionTest;

public class TracingOnlyTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ObservedApplication.class.getPackage())
                    .addClasses(TestTelemetry.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false")
            .overrideConfigKey("quarkus.modulith.observability.metrics.enabled", "false");

    @Inject
    OrderService orders;

    @Inject
    MeterRegistry meters;

    @Test
    void tracesWithoutMeters() {
        // arrange
        TestTelemetry.SPANS.reset();

        // act
        orders.complete("1", List.of(), new int[0]);

        // assert
        assertThat(TestTelemetry.SPANS.getFinishedSpanItems()).extracting(SpanData::getName)
                .containsExactlyInAnyOrder(
                        "Orders > i.q.m.o.d.a.o.OrderService.complete(j.l.String, j.u.List<j.l.String>, int[])",
                        "Inventory > i.q.m.o.d.a.i.InventoryListener.on(i.q.m.o.d.a.o.OrderCompleted)",
                        "inner");
        assertThat(meters.find("module.events.published").counters()).isEmpty();
        assertThat(meters.find("module.invocations").timers()).isEmpty();
    }
}
