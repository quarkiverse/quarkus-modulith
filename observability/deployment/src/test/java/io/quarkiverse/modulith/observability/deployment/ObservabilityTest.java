package io.quarkiverse.modulith.observability.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkiverse.modulith.observability.deployment.app.ObservedApplication;
import io.quarkiverse.modulith.observability.deployment.app.order.OrderService;
import io.quarkus.test.QuarkusExtensionTest;

public class ObservabilityTest {

    static final String ORDER_TYPE = "i.q.m.o.d.a.o.OrderService";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addPackages(true, ObservedApplication.class.getPackage())
                    .addClasses(TestTelemetry.class, TestCustomizers.class))
            .overrideConfigKey("quarkus.modulith.docs.enabled", "false");

    @Inject
    OrderService orders;

    @Inject
    MeterRegistry meters;

    @BeforeEach
    void reset() {
        TestTelemetry.SPANS.reset();
        meters.clear();
    }

    @Test
    void opensASpanPerModuleEntry() {
        // arrange
        List<String> lines = List.of("a");

        // act
        orders.complete("1", lines, new int[] { 1 });

        // assert
        Map<String, SpanData> spans = TestTelemetry.SPANS.getFinishedSpanItems().stream()
                .collect(Collectors.toMap(SpanData::getName, it -> it));
        SpanData order = spans.get("Orders > " + ORDER_TYPE + ".complete(j.l.String, j.u.List<j.l.String>, int[])");
        SpanData listener = spans.get("Inventory > i.q.m.o.d.a.i.InventoryListener.on(i.q.m.o.d.a.o.OrderCompleted)");
        assertThat(spans).hasSize(3);
        assertThat(order.getAttributes().asMap()).isEqualTo(Attributes.builder()
                .put("module.identifier", "order").put("module.name", "Orders")
                .put("module.invocation-type", "bean").put("module.method", "complete")
                .put("listener", false).build().asMap());
        assertThat(order.getParentSpanId()).isEqualTo("0000000000000000");
        assertThat(listener.getAttributes().get(AttributeKey.stringKey("module.invocation-type")))
                .isEqualTo("event-listener");
        assertThat(listener.getAttributes().get(AttributeKey.booleanKey("listener"))).isTrue();
        assertThat(listener.getParentSpanId()).isEqualTo(order.getSpanId());
        assertThat(spans.get("inner").getAttributes().get(AttributeKey.stringKey("module.identifier")))
                .isEqualTo("inventory");
        assertThat(spans.get("inner").getParentSpanId()).isEqualTo(listener.getSpanId());
    }

    @Test
    void timesInvocationsAndRecordsErrors() {
        // arrange
        OrderService service = orders;

        // act
        Throwable thrown = catchThrowable(service::fail);

        // assert
        SpanData span = TestTelemetry.SPANS.getFinishedSpanItems().get(0);
        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage("broken");
        assertThat(span.getName()).isEqualTo("Orders > " + ORDER_TYPE + ".fail()");
        assertThat(span.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
        assertThat(span.getEvents()).extracting(it -> it.getName()).containsExactly("exception");
        assertThat(meters.get("module.invocations").tags("module.identifier", "order", "module.name", "Orders",
                "module.invocation-type", "bean", "error", "IllegalStateException").timer().count()).isEqualTo(1);
    }

    @Test
    void countsEventsWithSpringModulithMeters() throws Exception {
        // arrange
        List<String> lines = List.of();

        // act
        orders.complete("7", lines, new int[0]);
        orders.cancel("8").toCompletableFuture().get(2, TimeUnit.SECONDS);

        // assert
        assertThat(meters.get("module.events.published").tags("event_type", "order.OrderCompleted",
                "module_identifier", "order", "module_name", "Orders").counter().count()).isEqualTo(1);
        assertThat(meters.get("module.events.published").tags("event_type", "order.OrderCancelled").counter()
                .count()).isEqualTo(1);
        assertThat(meters.get("module.events.published.order.OrderCompleted").tags("module_identifier", "order",
                "module_name", "Orders", "id", "7", "all", "yes").counter().count()).isEqualTo(1);
        assertThat(meters.get("orders.cancelled").tags("module_identifier", "order", "all", "yes").counter().count())
                .isEqualTo(1);
        assertThat(meters.find("module.events.published.order.OrderCancelled").counters()).isEmpty();
        assertThat(meters.get("module.invocations").tags("module.identifier", "inventory", "module.invocation-type",
                "event-listener", "error", "none").timer().count()).isEqualTo(1);
    }
}
